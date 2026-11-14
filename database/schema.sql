-- ============================================================================
-- RECIRCLE – A Trust-Based Campus Community Resource Sharing & Exchange System
-- Database Engine: MySQL 8.0+ / MySQL 9.5 (InnoDB, utf8mb4_unicode_ci)
-- Schema Name: recircle_db
-- All 21 Required Normalized Entities with Foreign Keys, Constraints & Indexes
-- ============================================================================

CREATE DATABASE IF NOT EXISTS recircle_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE recircle_db;

-- ----------------------------------------------------------------------------
-- 1. USERS TABLE
-- Stores core authentication credentials, role, campus verification, and status
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    registration_number VARCHAR(40) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    account_status VARCHAR(25) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    campus_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_reg_number UNIQUE (registration_number),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'MODERATOR', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION', 'DEACTIVATED'))
) ENGINE=InnoDB;

CREATE INDEX idx_users_role_status ON users(role, account_status);

-- ----------------------------------------------------------------------------
-- 2. USER_PROFILES TABLE
-- Stores academic & campus profile details (1:1 relationship with users)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    department VARCHAR(100) NOT NULL,
    year_of_study INT NOT NULL DEFAULT 1,
    contact_number VARCHAR(25) NOT NULL,
    hostel_or_block VARCHAR(100) NULL,
    bio VARCHAR(600) NULL,
    avatar_url VARCHAR(500) NULL,
    preferred_pickup_spot VARCHAR(150) NULL,
    profile_completion_pct INT NOT NULL DEFAULT 60,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_profile_year CHECK (year_of_study BETWEEN 1 AND 6)
) ENGINE=InnoDB;

CREATE INDEX idx_user_profiles_dept ON user_profiles(department);

-- ----------------------------------------------------------------------------
-- 3. CATEGORIES TABLE
-- Stores the 10 campus resource categories and administrative configuration
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(110) NOT NULL,
    description VARCHAR(350) NULL,
    icon_name VARCHAR(50) NOT NULL DEFAULT 'Package',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 4. ITEMS TABLE
-- Stores campus resource listings (Borrow, Rent, Exchange, Donate)
-- Includes optimistic locking version column to prevent concurrent race bookings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    subcategory VARCHAR(100) NULL,
    item_condition VARCHAR(25) NOT NULL,
    sharing_type VARCHAR(25) NOT NULL,
    availability_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    campus_location VARCHAR(150) NOT NULL,
    security_deposit DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    rental_price_per_day DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    available_from DATE NULL,
    available_until DATE NULL,
    max_borrow_days INT NOT NULL DEFAULT 14,
    exchange_preferences VARCHAR(400) NULL,
    usage_rules VARCHAR(500) NULL,
    primary_image_url VARCHAR(500) NULL,
    view_count INT NOT NULL DEFAULT 0,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_items_condition CHECK (item_condition IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR')),
    CONSTRAINT chk_items_sharing CHECK (sharing_type IN ('BORROW', 'RENT', 'EXCHANGE', 'DONATE')),
    CONSTRAINT chk_items_status CHECK (availability_status IN ('AVAILABLE', 'RESERVED', 'UNAVAILABLE', 'PENDING_APPROVAL', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT chk_items_prices CHECK (security_deposit >= 0 AND rental_price_per_day >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_items_discovery ON items(archived, availability_status, sharing_type, category_id);
CREATE INDEX idx_items_owner ON items(owner_id);
CREATE INDEX idx_items_created ON items(created_at DESC);

-- ----------------------------------------------------------------------------
-- 5. ITEM_IMAGES TABLE
-- Stores gallery photographs associated with each item listing
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS item_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    caption VARCHAR(160) NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_item_images_item ON item_images(item_id, display_order);

-- ----------------------------------------------------------------------------
-- 6. BORROW_REQUESTS TABLE
-- Manages borrowing and lending lifecycle with handover and return tracking
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrow_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    borrower_id BIGINT NOT NULL,
    lender_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    owner_response_note VARCHAR(400) NULL,
    handover_confirmed_at TIMESTAMP NULL,
    return_requested_at TIMESTAMP NULL,
    returned_at TIMESTAMP NULL,
    returned_on_time BOOLEAN NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_borrow_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_borrow_borrower FOREIGN KEY (borrower_id) REFERENCES users(id),
    CONSTRAINT fk_borrow_lender FOREIGN KEY (lender_id) REFERENCES users(id),
    CONSTRAINT chk_borrow_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_borrow_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'ACTIVE', 'RETURN_REQUESTED', 'COMPLETED', 'OVERDUE', 'DISPUTED'))
) ENGINE=InnoDB;

CREATE INDEX idx_borrow_item_dates ON borrow_requests(item_id, status, start_date, end_date);
CREATE INDEX idx_borrow_borrower ON borrow_requests(borrower_id, status);
CREATE INDEX idx_borrow_lender ON borrow_requests(lender_id, status);

-- ----------------------------------------------------------------------------
-- 7. EXCHANGE_OFFERS TABLE
-- Manages peer-to-peer item exchange proposals and dual handover confirmation
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exchange_offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requested_item_id BIGINT NOT NULL,
    offered_item_id BIGINT NOT NULL,
    proposer_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    proposal_message VARCHAR(600) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    proposer_handover_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    receiver_handover_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_exchange_req_item FOREIGN KEY (requested_item_id) REFERENCES items(id),
    CONSTRAINT fk_exchange_off_item FOREIGN KEY (offered_item_id) REFERENCES items(id),
    CONSTRAINT fk_exchange_proposer FOREIGN KEY (proposer_id) REFERENCES users(id),
    CONSTRAINT fk_exchange_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT chk_exchange_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'HANDOVER_PENDING', 'COMPLETED', 'DISPUTED'))
) ENGINE=InnoDB;

CREATE INDEX idx_exchange_items ON exchange_offers(requested_item_id, offered_item_id, status);
CREATE INDEX idx_exchange_users ON exchange_offers(proposer_id, receiver_id);

-- ----------------------------------------------------------------------------
-- 8. RENTALS TABLE
-- Manages paid campus rentals with backend cost calculation & simulated payment
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rentals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    renter_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    rental_days INT NOT NULL,
    daily_rate DECIMAL(10, 2) NOT NULL,
    security_deposit DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_rental_amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    handover_confirmed_at TIMESTAMP NULL,
    returned_at TIMESTAMP NULL,
    returned_on_time BOOLEAN NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rentals_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_rentals_renter FOREIGN KEY (renter_id) REFERENCES users(id),
    CONSTRAINT fk_rentals_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT chk_rentals_dates CHECK (end_date >= start_date AND rental_days >= 1),
    CONSTRAINT chk_rentals_payment CHECK (payment_status IN ('NOT_REQUIRED', 'PENDING', 'PAID_SIMULATED', 'REFUNDED_SIMULATED', 'FAILED')),
    CONSTRAINT chk_rentals_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'ACTIVE', 'COMPLETED', 'OVERDUE', 'DISPUTED'))
) ENGINE=InnoDB;

CREATE INDEX idx_rentals_item_dates ON rentals(item_id, status, start_date, end_date);
CREATE INDEX idx_rentals_users ON rentals(renter_id, owner_id);

-- ----------------------------------------------------------------------------
-- 9. DONATION_REQUESTS TABLE
-- Manages zero-cost student donations and recipient selection
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donation_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    motivation_message VARCHAR(600) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    handover_confirmed_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_donation_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_donation_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_donation_donor FOREIGN KEY (donor_id) REFERENCES users(id),
    CONSTRAINT chk_donation_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'))
) ENGINE=InnoDB;

CREATE INDEX idx_donation_item ON donation_requests(item_id, status);

-- ----------------------------------------------------------------------------
-- 10. TRANSACTIONS TABLE
-- Immutable unified ledger tracking all resource-sharing workflows across campus
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_code VARCHAR(40) NOT NULL,
    transaction_type VARCHAR(25) NOT NULL,
    reference_id BIGINT NOT NULL,
    primary_item_id BIGINT NOT NULL,
    secondary_item_id BIGINT NULL,
    initiator_user_id BIGINT NOT NULL,
    counterparty_user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    monetary_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    start_date DATE NULL,
    due_date DATE NULL,
    completed_at TIMESTAMP NULL,
    returned_on_time BOOLEAN NULL,
    notes VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_transactions_code UNIQUE (transaction_code),
    CONSTRAINT fk_tx_primary_item FOREIGN KEY (primary_item_id) REFERENCES items(id),
    CONSTRAINT fk_tx_initiator FOREIGN KEY (initiator_user_id) REFERENCES users(id),
    CONSTRAINT fk_tx_counterparty FOREIGN KEY (counterparty_user_id) REFERENCES users(id),
    CONSTRAINT chk_tx_type CHECK (transaction_type IN ('BORROW', 'RENT', 'EXCHANGE', 'DONATE'))
) ENGINE=InnoDB;

CREATE INDEX idx_tx_users ON transactions(initiator_user_id, counterparty_user_id, status);
CREATE INDEX idx_tx_type_status ON transactions(transaction_type, status, created_at);

-- ----------------------------------------------------------------------------
-- 11. REVIEWS TABLE
-- Stores verified post-transaction 5-star ratings and written feedback
-- Unique constraint prevents duplicate reviews per transaction and reviewer
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(800) NOT NULL,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason VARCHAR(300) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_tx_reviewer UNIQUE (transaction_id, reviewer_id),
    CONSTRAINT fk_reviews_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_reviews_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES users(id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_not_self CHECK (reviewer_id <> reviewee_id)
) ENGINE=InnoDB;

CREATE INDEX idx_reviews_reviewee ON reviews(reviewee_id, created_at DESC);

-- ----------------------------------------------------------------------------
-- 12. TRUST_SCORES TABLE
-- Stores current normalized trust score (0-100) and 4 weighted components
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trust_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_score DECIMAL(5, 2) NOT NULL DEFAULT 50.00,
    trust_level VARCHAR(25) NOT NULL DEFAULT 'DEVELOPING',
    verified_account_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    completed_tx_score DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    user_review_score DECIMAL(5, 2) NOT NULL DEFAULT 50.00,
    on_time_return_score DECIMAL(5, 2) NOT NULL DEFAULT 50.00,
    completed_transactions_count INT NOT NULL DEFAULT 0,
    average_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    on_time_return_percentage DECIMAL(5, 2) NOT NULL DEFAULT 50.00,
    eligible_return_count INT NOT NULL DEFAULT 0,
    on_time_return_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_trust_scores_user UNIQUE (user_id),
    CONSTRAINT fk_trust_scores_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_trust_total CHECK (total_score BETWEEN 0.00 AND 100.00),
    CONSTRAINT chk_trust_level CHECK (trust_level IN ('EXCELLENT', 'TRUSTED', 'DEVELOPING', 'LOW_TRUST'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 13. TRUST_SCORE_HISTORY TABLE
-- Stores transparent audit trail of every trust score recalculation event
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trust_score_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    previous_score DECIMAL(5, 2) NOT NULL,
    new_score DECIMAL(5, 2) NOT NULL,
    delta DECIMAL(5, 2) NOT NULL,
    trigger_event VARCHAR(80) NOT NULL,
    reason_summary VARCHAR(300) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trust_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_trust_history_user ON trust_score_history(user_id, created_at DESC);

-- ----------------------------------------------------------------------------
-- 14. NOTIFICATIONS TABLE
-- Database-backed in-app notifications with deep links and read status
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_path VARCHAR(200) NULL,
    related_entity_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read, created_at DESC);

-- ----------------------------------------------------------------------------
-- 15. DISPUTES TABLE
-- Tracks transaction disputes, moderation status, and resolution decisions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS disputes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NULL,
    item_id BIGINT NULL,
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_moderator_id BIGINT NULL,
    resolution_notes TEXT NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_disputes_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_disputes_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_disputes_reported FOREIGN KEY (reported_user_id) REFERENCES users(id),
    CONSTRAINT chk_disputes_status CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED', 'ESCALATED'))
) ENGINE=InnoDB;

CREATE INDEX idx_disputes_status ON disputes(status, created_at DESC);

-- ----------------------------------------------------------------------------
-- 16. DISPUTE_EVIDENCE TABLE
-- Stores evidence attachments and investigation comments on disputes
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dispute_evidence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispute_id BIGINT NOT NULL,
    submitted_by_id BIGINT NOT NULL,
    evidence_type VARCHAR(40) NOT NULL DEFAULT 'COMMENT',
    evidence_url VARCHAR(500) NULL,
    comment TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evidence_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(id) ON DELETE CASCADE,
    CONSTRAINT fk_evidence_user FOREIGN KEY (submitted_by_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 17. REPORTS TABLE
-- Stores flagged listings, reviews, or suspicious account reports
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details VARCHAR(600) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    moderator_note VARCHAR(400) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT chk_reports_target CHECK (target_type IN ('ITEM', 'USER', 'REVIEW'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 18. AUDIT_LOGS TABLE
-- Immutable audit log of administrative and moderator actions
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    actor_role VARCHAR(25) NOT NULL,
    action_type VARCHAR(80) NOT NULL,
    target_entity VARCHAR(50) NOT NULL,
    target_id BIGINT NULL,
    details VARCHAR(600) NOT NULL,
    ip_address VARCHAR(60) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);

-- ----------------------------------------------------------------------------
-- 19. REFRESH_TOKENS TABLE
-- Manages revocable JWT refresh tokens
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 20. EMAIL_VERIFICATION_TOKENS TABLE
-- Stores OTP codes / verification tokens for campus email verification
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    otp_code VARCHAR(12) NOT NULL,
    verification_token VARCHAR(120) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verify_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 21. PASSWORD_RESET_TOKENS TABLE
-- Stores time-bound tokens for secure password recovery
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reset_token VARCHAR(120) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_reset_token UNIQUE (reset_token),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
