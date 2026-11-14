package com.recircle.service;

import com.recircle.dto.ApiDtos.*;
import com.recircle.entity.*;
import com.recircle.enums.*;
import com.recircle.exception.ApiException;
import com.recircle.mapper.DtoMapper;
import com.recircle.repository.*;
import com.recircle.validation.InputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ItemImageRepository itemImageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final InputValidator inputValidator;
    private final DtoMapper dtoMapper;

    public ItemService(
        ItemRepository itemRepository,
        CategoryRepository categoryRepository,
        ItemImageRepository itemImageRepository,
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        TrustScoreRepository trustScoreRepository,
        InputValidator inputValidator,
        DtoMapper dtoMapper
    ) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.itemImageRepository = itemImageRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.trustScoreRepository = trustScoreRepository;
        this.inputValidator = inputValidator;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories() {
        return categoryRepository.findByActiveTrueOrderByIdAsc()
            .stream()
            .map(dtoMapper::toCategoryDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchAndFilterItems(
        String query,
        Long categoryId,
        String department,
        String sharingType,
        String condition,
        String availability,
        BigDecimal maxPrice,
        String sortBy,
        int page,
        int size
    ) {
        List<ItemDto> all = itemRepository.findByArchivedFalseOrderByCreatedAtDesc()
            .stream()
            .map(dtoMapper::toItemDto)
            .filter(item -> {
                if (query != null && !query.isBlank()) {
                    String q = query.trim().toLowerCase();
                    boolean matchTitle = item.title() != null && item.title().toLowerCase().contains(q);
                    boolean matchDesc = item.description() != null && item.description().toLowerCase().contains(q);
                    boolean matchCat = item.categoryName() != null && item.categoryName().toLowerCase().contains(q);
                    boolean matchLoc = item.campusLocation() != null && item.campusLocation().toLowerCase().contains(q);
                    if (!matchTitle && !matchDesc && !matchCat && !matchLoc) {
                        return false;
                    }
                }
                if (categoryId != null && !categoryId.equals(item.categoryId())) {
                    return false;
                }
                if (department != null && !department.isBlank() && !"ALL".equalsIgnoreCase(department)) {
                    if (item.ownerDepartment() == null || !item.ownerDepartment().toLowerCase().contains(department.trim().toLowerCase())) {
                        return false;
                    }
                }
                if (sharingType != null && !sharingType.isBlank() && !"ALL".equalsIgnoreCase(sharingType)) {
                    if (!sharingType.equalsIgnoreCase(item.sharingType())) {
                        return false;
                    }
                }
                if (condition != null && !condition.isBlank() && !"ALL".equalsIgnoreCase(condition)) {
                    if (!condition.equalsIgnoreCase(item.itemCondition())) {
                        return false;
                    }
                }
                if (availability != null && !availability.isBlank() && !"ALL".equalsIgnoreCase(availability)) {
                    if (!availability.equalsIgnoreCase(item.availabilityStatus())) {
                        return false;
                    }
                }
                if (maxPrice != null && item.rentalPricePerDay() != null) {
                    if (item.rentalPricePerDay().compareTo(maxPrice) > 0) {
                        return false;
                    }
                }
                return true;
            })
            .toList();

        List<ItemDto> sorted = new ArrayList<>(all);
        String sortKey = sortBy != null ? sortBy.toLowerCase() : "newest";
        switch (sortKey) {
            case "oldest" -> sorted.sort(Comparator.comparing(ItemDto::id));
            case "rating" -> sorted.sort(Comparator.comparing(ItemDto::ownerAverageRating, Comparator.nullsLast(Comparator.reverseOrder())));
            case "price_asc" -> sorted.sort(Comparator.comparing(ItemDto::rentalPricePerDay, Comparator.nullsLast(Comparator.naturalOrder())));
            case "trust_score" -> sorted.sort(Comparator.comparing(ItemDto::ownerTrustScore, Comparator.nullsLast(Comparator.reverseOrder())));
            default -> sorted.sort(Comparator.comparing(ItemDto::id, Comparator.reverseOrder()));
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        int totalElements = sorted.size();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<ItemDto> pageContent = sorted.subList(fromIndex, toIndex);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", pageContent);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        return result;
    }

    @Transactional
    public Map<String, Object> getItemDetails(Long itemId) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item listing not found."));
        item.setViewCount(item.getViewCount() + 1);
        itemRepository.save(item);

        ItemDto dto = dtoMapper.toItemDto(item);
        List<ItemDto> related = itemRepository.findByCategoryIdAndArchivedFalseAndIdNotOrderByCreatedAtDesc(
                item.getCategory().getId(), item.getId()
            )
            .stream()
            .limit(4)
            .map(dtoMapper::toItemDto)
            .toList();

        return Map.of(
            "item", dto,
            "relatedItems", related
        );
    }

    @Transactional(readOnly = true)
    public List<ItemDto> getMyListings(Long ownerId) {
        return itemRepository.findByOwnerIdAndArchivedFalseOrderByCreatedAtDesc(ownerId)
            .stream()
            .map(dtoMapper::toItemDto)
            .toList();
    }

    @Transactional
    public ItemDto createItem(Long ownerId, ItemUpsertRequest req) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Owner not found."));

        validateItemRequest(req);

        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Selected category does not exist."));

        Item item = new Item();
        item.setOwner(owner);
        item.setCategory(category);
        applyUpsertFields(item, req);

        Item saved = itemRepository.save(item);
        saveGalleryImages(saved, req.galleryUrls());
        return dtoMapper.toItemDto(saved);
    }

    @Transactional
    public ItemDto updateItem(Long itemId, Long actorUserId, UserRole actorRole, ItemUpsertRequest req) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item listing not found."));

        if (!item.getOwner().getId().equals(actorUserId) && actorRole != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only edit your own item listings.");
        }

        validateItemRequest(req);

        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Selected category does not exist."));

        item.setCategory(category);
        applyUpsertFields(item, req);

        Item saved = itemRepository.save(item);
        if (req.galleryUrls() != null) {
            itemImageRepository.deleteByItemId(saved.getId());
            saveGalleryImages(saved, req.galleryUrls());
        }
        return dtoMapper.toItemDto(saved);
    }

    @Transactional
    public void archiveItem(Long itemId, Long actorUserId, UserRole actorRole) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item listing not found."));

        if (!item.getOwner().getId().equals(actorUserId) && actorRole != UserRole.ADMIN && actorRole != UserRole.MODERATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only delete or archive your own listings.");
        }

        item.setArchived(true);
        item.setAvailabilityStatus(ListingStatus.ARCHIVED);
        itemRepository.save(item);
    }

    private void validateItemRequest(ItemUpsertRequest req) {
        inputValidator.requireNonBlank(req.title(), "Item title is required.");
        inputValidator.requireNonBlank(req.description(), "Item description is required.");
        if (req.categoryId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category is required.");
        }
        inputValidator.requireNonBlank(req.itemCondition(), "Item condition is required.");
        inputValidator.requireNonBlank(req.sharingType(), "Sharing type is required.");
        inputValidator.requireNonBlank(req.campusLocation(), "Campus location is required.");

        if (req.rentalPricePerDay() != null && req.rentalPricePerDay().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rental price cannot be negative.");
        }
        if (req.securityDeposit() != null && req.securityDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Security deposit cannot be negative.");
        }
        if ("RENT".equalsIgnoreCase(req.sharingType())) {
            if (req.rentalPricePerDay() == null || req.rentalPricePerDay().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Rental listings require a positive daily rental price.");
            }
        }
    }

    private void applyUpsertFields(Item item, ItemUpsertRequest req) {
        item.setTitle(inputValidator.sanitizeText(req.title()));
        item.setDescription(inputValidator.sanitizeText(req.description()));
        item.setSubcategory(inputValidator.sanitizeText(req.subcategory()));
        item.setItemCondition(ItemCondition.valueOf(req.itemCondition().toUpperCase()));
        item.setSharingType(SharingType.valueOf(req.sharingType().toUpperCase()));
        if (req.availabilityStatus() != null && !req.availabilityStatus().isBlank()) {
            item.setAvailabilityStatus(ListingStatus.valueOf(req.availabilityStatus().toUpperCase()));
        }
        item.setCampusLocation(inputValidator.sanitizeText(req.campusLocation()));
        item.setSecurityDeposit(req.securityDeposit() != null ? req.securityDeposit() : BigDecimal.ZERO);
        item.setRentalPricePerDay(req.rentalPricePerDay() != null ? req.rentalPricePerDay() : BigDecimal.ZERO);
        if (req.availableFrom() != null && !req.availableFrom().isBlank()) {
            item.setAvailableFrom(LocalDate.parse(req.availableFrom()));
        }
        if (req.availableUntil() != null && !req.availableUntil().isBlank()) {
            item.setAvailableUntil(LocalDate.parse(req.availableUntil()));
        }
        item.setMaxBorrowDays(req.maxBorrowDays() != null && req.maxBorrowDays() > 0 ? req.maxBorrowDays() : 14);
        item.setExchangePreferences(inputValidator.sanitizeText(req.exchangePreferences()));
        item.setUsageRules(inputValidator.sanitizeText(req.usageRules()));

        String defaultImg = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80";
        item.setPrimaryImageUrl(req.primaryImageUrl() != null && !req.primaryImageUrl().isBlank() ? req.primaryImageUrl().trim() : defaultImg);
    }

    private void saveGalleryImages(Item item, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            ItemImage img = new ItemImage();
            img.setItem(item);
            img.setImageUrl(item.getPrimaryImageUrl());
            img.setDisplayOrder(0);
            itemImageRepository.save(img);
            return;
        }
        int order = 0;
        for (String url : urls) {
            if (url != null && !url.isBlank()) {
                ItemImage img = new ItemImage();
                img.setItem(item);
                img.setImageUrl(url.trim());
                img.setDisplayOrder(order++);
                itemImageRepository.save(img);
            }
        }
    }
}
