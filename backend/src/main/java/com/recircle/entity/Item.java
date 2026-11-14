package com.recircle.entity;

import com.recircle.enums.ItemCondition;
import com.recircle.enums.ListingStatus;
import com.recircle.enums.SharingType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "subcategory", length = 100)
    private String subcategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false, length = 25)
    private ItemCondition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_type", nullable = false, length = 25)
    private SharingType sharingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    private ListingStatus availabilityStatus = ListingStatus.AVAILABLE;

    @Column(name = "campus_location", nullable = false, length = 150)
    private String campusLocation;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "landmark", length = 150)
    private String landmark;

    @Column(name = "pickup_instructions", length = 300)
    private String pickupInstructions;

    @Column(name = "security_deposit", nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @Column(name = "rental_price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal rentalPricePerDay = BigDecimal.ZERO;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "available_until")
    private LocalDate availableUntil;

    @Column(name = "max_borrow_days", nullable = false)
    private Integer maxBorrowDays = 14;

    @Column(name = "exchange_preferences", length = 400)
    private String exchangePreferences;

    @Column(name = "usage_rules", length = 500)
    private String usageRules;

    @Column(name = "primary_image_url", length = 500)
    private String primaryImageUrl;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public ItemCondition getItemCondition() { return itemCondition; }
    public void setItemCondition(ItemCondition itemCondition) { this.itemCondition = itemCondition; }
    public SharingType getSharingType() { return sharingType; }
    public void setSharingType(SharingType sharingType) { this.sharingType = sharingType; }
    public ListingStatus getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(ListingStatus availabilityStatus) { this.availabilityStatus = availabilityStatus; }
    public String getCampusLocation() { return campusLocation; }
    public void setCampusLocation(String campusLocation) { this.campusLocation = campusLocation; }
    public String getLocation() { return campusLocation; }
    public void setLocation(String location) { this.campusLocation = location; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }
    public String getPickupInstructions() { return pickupInstructions; }
    public void setPickupInstructions(String pickupInstructions) { this.pickupInstructions = pickupInstructions; }
    public BigDecimal getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(BigDecimal securityDeposit) { this.securityDeposit = securityDeposit; }
    public BigDecimal getRentalPricePerDay() { return rentalPricePerDay; }
    public void setRentalPricePerDay(BigDecimal rentalPricePerDay) { this.rentalPricePerDay = rentalPricePerDay; }
    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }
    public LocalDate getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(LocalDate availableUntil) { this.availableUntil = availableUntil; }
    public Integer getMaxBorrowDays() { return maxBorrowDays; }
    public void setMaxBorrowDays(Integer maxBorrowDays) { this.maxBorrowDays = maxBorrowDays; }
    public String getExchangePreferences() { return exchangePreferences; }
    public void setExchangePreferences(String exchangePreferences) { this.exchangePreferences = exchangePreferences; }
    public String getUsageRules() { return usageRules; }
    public void setUsageRules(String usageRules) { this.usageRules = usageRules; }
    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
