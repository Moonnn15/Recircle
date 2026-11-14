package com.recircle.controller;

import com.recircle.dto.ApiDtos.*;
import com.recircle.exception.ApiException;
import com.recircle.security.AuthContext;
import com.recircle.security.AuthPrincipal;
import com.recircle.service.ItemService;
import com.recircle.service.ReviewDisputeService;
import com.recircle.service.SharingWorkflowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ItemCategoryController {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L; // 5 MB

    private final ItemService itemService;
    private final ReviewDisputeService reviewDisputeService;
    private final SharingWorkflowService sharingWorkflowService;
    private final String uploadDir;

    public ItemCategoryController(
        ItemService itemService,
        ReviewDisputeService reviewDisputeService,
        SharingWorkflowService sharingWorkflowService,
        @Value("${recircle.storage.upload-dir:./uploads}") String uploadDir
    ) {
        this.itemService = itemService;
        this.reviewDisputeService = reviewDisputeService;
        this.sharingWorkflowService = sharingWorkflowService;
        this.uploadDir = uploadDir;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(itemService.getCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryUpsertRequest req) {
        AuthContext.requireRole(com.recircle.enums.UserRole.ADMIN, com.recircle.enums.UserRole.MODERATOR);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.createCategory(req));
    }

    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> listItems(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String department,
        @RequestParam(required = false) String sharingType,
        @RequestParam(required = false) String condition,
        @RequestParam(required = false) String availability,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(defaultValue = "newest") String sortBy,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(itemService.searchAndFilterItems(
            query, categoryId, department, sharingType, condition, availability, maxPrice, sortBy, page, size
        ));
    }

    @GetMapping("/items/my-listings")
    public ResponseEntity<List<ItemDto>> getMyListings() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(itemService.getMyListings(p.getUserId()));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<Map<String, Object>> getItemDetails(@PathVariable Long id) {
        Map<String, Object> base = itemService.getItemDetails(id);
        Map<String, Object> availability = sharingWorkflowService.checkItemDateAvailability(id, null, null);
        return ResponseEntity.ok(Map.of(
            "item", base.get("item"),
            "relatedItems", base.get("relatedItems"),
            "reviews", reviewDisputeService.getItemReviews(id),
            "reservedDateRanges", availability.get("reservedDateRanges")
        ));
    }

    @GetMapping("/items/{id}/availability")
    public ResponseEntity<Map<String, Object>> checkItemAvailability(
        @PathVariable Long id,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        return ResponseEntity.ok(sharingWorkflowService.checkItemDateAvailability(id, startDate, endDate));
    }

    @PostMapping("/items")
    public ResponseEntity<ItemDto> createItem(@RequestBody ItemUpsertRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.createItem(p.getUserId(), req));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ItemDto> updateItem(@PathVariable Long id, @RequestBody ItemUpsertRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(itemService.updateItem(id, p.getUserId(), p.getRole(), req));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Map<String, String>> deleteItem(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        itemService.archiveItem(id, p.getUserId(), p.getRole());
        return ResponseEntity.ok(Map.of("message", "Item listing archived successfully."));
    }

    @PostMapping("/items/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        AuthContext.requireAuth();
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Please select an image file to upload.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "Image size exceeds the 5 MB limit.");
        }
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE_TYPE",
                "Only JPEG, PNG, and WebP images are permitted."
            );
        }

        try {
            Path dirPath = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dirPath);
            String ext = contentType.contains("png") ? ".png" : contentType.contains("webp") ? ".webp" : ".jpg";
            String filename = "rc_img_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path filePath = dirPath.resolve(filename);
            Files.write(filePath, file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "imageUrl", "/uploads/" + filename,
                "message", "Image uploaded and validated successfully."
            ));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded image.");
        }
    }
}
