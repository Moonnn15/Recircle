package com.recircle.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "year_of_study", nullable = false)
    private Integer yearOfStudy = 1;

    @Column(name = "contact_number", nullable = false, length = 25)
    private String contactNumber;

    @Column(name = "hostel_or_block", length = 100)
    private String hostelOrBlock;

    @Column(name = "bio", length = 600)
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "preferred_pickup_spot", length = 150)
    private String preferredPickupSpot;

    @Column(name = "profile_completion_pct", nullable = false)
    private Integer profileCompletionPct = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public int calculateCompletion() {
        int score = 50; // Base registration fields (name, email, reg no, dept, year, phone)
        if (hostelOrBlock != null && !hostelOrBlock.isBlank()) score += 15;
        if (bio != null && !bio.isBlank()) score += 15;
        if (preferredPickupSpot != null && !preferredPickupSpot.isBlank()) score += 10;
        if (avatarUrl != null && !avatarUrl.isBlank()) score += 10;
        this.profileCompletionPct = Math.min(100, score);
        return this.profileCompletionPct;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getHostelOrBlock() { return hostelOrBlock; }
    public void setHostelOrBlock(String hostelOrBlock) { this.hostelOrBlock = hostelOrBlock; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPreferredPickupSpot() { return preferredPickupSpot; }
    public void setPreferredPickupSpot(String preferredPickupSpot) { this.preferredPickupSpot = preferredPickupSpot; }
    public Integer getProfileCompletionPct() { return profileCompletionPct; }
    public void setProfileCompletionPct(Integer profileCompletionPct) { this.profileCompletionPct = profileCompletionPct; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
