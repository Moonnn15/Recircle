# ReCircle — Software Testing & Verification Report

## 1. Toolchain & Execution Environment
- **Java Runtime & Compiler**: OpenJDK `21.0.6` (`C:\Program Files\Android\Android Studio\jbr`)
- **Backend Framework**: Spring Boot `3.2.5`, Hibernate `6.4.4.Final`, JUnit Jupiter `5.10.2`, MockMvc
- **Database Engine Verified**: MySQL Server `9.5.0` (`C:\Program Files\MySQL\MySQL Server 9.5\bin\mysqld.exe`)
- **Automated JUnit 5 Integration Methods Executed**: `30 / 30 Passed` (`100% Pass Rate`, `0 Failures`, `0 Errors`)

---

## 2. Genuine MySQL 9.5.0 Verification Results
Executed `recircle/database/schema.sql` (twice back-to-back to verify idempotency) and `recircle/database/seed.sql` directly on **MySQL Server 9.5.0**:
- `MYSQL_VERSION = 9.5.0`
- `TABLE_COUNT = 21` (`InnoDB`, `utf8mb4_0900_ai_ci`)
- `CHECK_CONSTRAINT_COUNT = 20` (`ENFORCED_CHECK_COUNT = 20`)
- `FK_CONSTRAINT_COUNT = 37`
- `UNIQUE_CONSTRAINT_COUNT = 10`
- `USERS_SEEDED = 6`, `CATEGORIES_SEEDED = 10`, `ITEMS_SEEDED = 8`, `BORROW_SEEDED = 2`, `RENTALS_SEEDED = 1`, `EXCHANGES_SEEDED = 1`, `DONATIONS_SEEDED = 1`, `TRANSACTIONS_SEEDED = 3`, `REVIEWS_SEEDED = 2`, `TRUST_HISTORY_SEEDED = 2`
- `MySQL 9.5 Error Count = 0`

---

## 3. Executed JUnit 5 Test Suite Matrix (`TC01`–`TC30` + Business Rule Verification)

| Test ID | Story / Rule | Test Scenario | Expected Result | Actual Result | Status |
| :---: | :---: | :--- | :--- | :--- | :---: |
| **TC01** | `US01` | Valid student registration with `@srmist.edu.in` email | HTTP `201 Created`, BCrypt password stored, JWT & OTP issued | HTTP `201 Created` | **PASS** |
| **TC02** | `US01` | Non-campus email domain rejection (`external@gmail.com`) | HTTP `400 Bad Request` (`INVALID_CAMPUS_DOMAIN`) | HTTP `400 Bad Request` | **PASS** |
| **TC03** | `US01` | Duplicate email / registration number prevention | HTTP `409 Conflict` (`DUPLICATE_EMAIL`) | HTTP `409 Conflict` | **PASS** |
| **TC04** | `US01` | Weak password rejection (`"short"`) | HTTP `400 Bad Request` (`WEAK_PASSWORD`) | HTTP `400 Bad Request` | **PASS** |
| **TC05** | `US02` | Valid login for seeded Student & Admin accounts | HTTP `200 OK` with signed JWT access & refresh tokens | HTTP `200 OK` | **PASS** |
| **TC06** | `US02` | Invalid login password rejection | HTTP `401 Unauthorized` | HTTP `401 Unauthorized` | **PASS** |
| **TC07** | `US02` | Unauthenticated request to `/api/v1/users/me` blocked | HTTP `401 Unauthorized` | HTTP `401 Unauthorized` | **PASS** |
| **TC08** | `US05` | RBAC: Student blocked from `/api/v1/admin/dashboard` | HTTP `403 Forbidden` | HTTP `403 Forbidden` | **PASS** |
| **TC09** | `US06` | Create listings across `BORROW`, `RENT`, and `DONATE` | HTTP `201 Created` with `AVAILABLE` status | HTTP `201 Created` | **PASS** |
| **TC10** | `US05`/`US06` | Owner edits listing (`200 OK`); non-owner edit blocked (`403 Forbidden`) | Ownership check enforced | HTTP `200` / `403` | **PASS** |
| **TC11** | `US06` | Soft-archive listing (`DELETE /api/v1/items/{id}`) | Status set to `ARCHIVED` and excluded from discovery search | HTTP `200 OK`, `totalElements = 0` | **PASS** |
| **TC12** | `US07` | Keyword search, category/sharingType filters, sort & pagination | Returns matching non-archived items with pagination metadata | HTTP `200 OK` | **PASS** |
| **TC13** | `3.D` | Self-borrow request rejected (`SELF_REQUEST_FORBIDDEN`) | HTTP `400 Bad Request` | HTTP `400 Bad Request` | **PASS** |
| **TC14** | `3.A` | Overlapping date reservation rejected (`OVERLAPPING_RESERVATION`) | HTTP `409 Conflict` | HTTP `409 Conflict` | **PASS** |
| **TC15** | `3.A` | Non-overlapping future dates on the SAME item remain bookable | HTTP `201 Created` even while earlier booking is `APPROVED` | HTTP `201 Created` | **PASS** |
| **TC16** | `3.D` | Cancellation of booking transitions to `CANCELLED` & updates ledger | HTTP `200 OK`, `status = CANCELLED` | HTTP `200 OK` | **PASS** |
| **TC17** | `3.D` | Full borrow lifecycle (`PENDING -> APPROVED -> ACTIVE -> RETURN_REQUESTED -> COMPLETED`) | Completed on-time return updates ledger and recalculates trust scores | HTTP `200 OK`, `COMPLETED` | **PASS** |
| **TC18** | `3.D` | Exchange proposal, acceptance, and completion | Both items transition to `COMPLETED` and trust scores update | HTTP `200 OK`, `COMPLETED` | **PASS** |
| **TC19** | `3.B` | Rental cost calculation (`4 days * ₹50 = ₹200`) & demo payment mode | `rentalDays = 4`, `totalRentalAmount = 200.00` | Verified | **PASS** |
| **TC20** | `3.D` | Donation approval & single-recipient enforcement (`ALREADY_ASSIGNED`) | Second assignment attempt rejected with HTTP `409 Conflict` | HTTP `409 Conflict` | **PASS** |
| **TC21** | `US03`/`3.C` | Email OTP verification (`+15` pts), 5-star review & duplicate review block | HTTP `201 Created` on first review; HTTP `409 Conflict` on duplicate | Verified | **PASS** |
| **TC22** | `US06` | Image upload MIME & size validation (`.exe` rejected, `.png` accepted) | HTTP `400` (`INVALID_FILE_TYPE`) vs HTTP `201 Created` | Verified | **PASS** |
| **TC28** | Phase 0 / `3.A` / `3.B` / `3.C` / `US03` | Health check (`GET /api/v1/health`), single-use OTP reuse rejection, rental approval keeping `paymentStatus = PENDING`, date-range availability endpoint, and `-15.00` resolved dispute penalty | All assertions verified on JDK `21.0.6` | `24/24` Passed | **PASS** |
| **TC31** | `US08`–`US10` | Duplicate pending borrow rejection (`409 DUPLICATE_BORROW_REQUEST`), unauthorized borrow approval blocked (`403 Forbidden`), participant-only borrow details (`GET /api/v1/borrow-requests/{id}`), owner rejection with notes, and late return calculation (`returnedOnTime = false`) | All status codes, owner notes, and `returnedOnTime = false` verified | HTTP `409`/`403`/`200` | **PASS** |
| **TC32** | `US11` | Exchange proposal creation, ascending-ID lock order, exchange cancellation (`CANCELLED`), acceptance (`ACCEPTED`), and dual handover confirmation (`HANDOVER_PENDING -> COMPLETED`) | Both participants must confirm handover before completion (`400 DUAL_HANDOVER_REQUIRED` on unilateral `/complete`) | HTTP `201`/`200` | **PASS** |
| **TC33** | `US12` | Backend rental quote calculation (`POST` & `GET /api/v1/rentals/calculate`), unpaid rental approval, handover (`ACTIVE`), and return completion (`COMPLETED`) | `5 days * ₹50 = ₹250 + ₹500 deposit = ₹750`; `paymentStatus` remains `PENDING` throughout | HTTP `200 OK` | **PASS** |
| **TC34** | `US13` | Multiple competing donation requests for the same donated item: approving one recipient atomically rejects all competing `PENDING` requests | Approved request becomes `APPROVED`; competing request transitions atomically to `REJECTED` | HTTP `200 OK` | **PASS** |
| **TC35** | `US14`–`US15` | Unified transaction history deduplication across `BORROW`, `RENT`, `EXCHANGE`, `DONATE` + individual notification mark-as-read (`PUT /api/v1/notifications/{id}/read`) | All 4 workflow types present with unique `transactionCode`s; `unreadCount` decrements by `1` | HTTP `200 OK` | **PASS** |
| **TC36** | `US08`–`US09` | Multi-threaded concurrent approval protection (`ExecutorService` + `CountDownLatch`) for two overlapping borrow requests on the same item | Exactly 1 approval succeeds (`200 OK`) and the competing concurrent approval is rejected | `successCount == 1` | **PASS** |
| **TC37** | `US16` | 4-pillar dynamic trust score calculation, neutral 50.0 baselines, email verification delta (+15 pts), and mathematical bounds [0, 100] | Fresh unverified = 25.00; Verified = 40.00; HALF_UP rounding | HTTP `200 OK` | **PASS** |
| **TC38** | `US17` | Peer reviews: Non-participant review attempt blocked (403), 1–5 star rating validation (400), and public review feed | Participant-only review authorized, rating validated | HTTP `403`/`400`/`200` | **PASS** |
| **TC39** | `US18`–`US19` | Dispute filing, admin resolution with -15.00 penalty, and repeated resolution prevention (`400 DISPUTE_ALREADY_RESOLVED`) | Resolution applies penalty; second resolution attempt blocked | HTTP `201`/`200`/`400` | **PASS** |
| **TC40** | `US20`–`US21` | Admin dashboard KPIs and Recharts analytics datasets (sharing types, categories, monthly trends, trust score histogram) | Student access blocked (403); Admin receives accurate real-time aggregates | HTTP `403`/`200` | **PASS** |
| **TC41** | `US22` | Admin user directory, student account suspension (`SUSPENDED`), and reactivation (`ACTIVE`) | Non-admin suspension attempt blocked (403); status toggles verified | HTTP `403`/`200` | **PASS** |
| **TC42** | `US23` | Immutable administrative audit trail, action filters, and verification of zero credential or token leakage | Student blocked (403); Admin retrieves logs; no passwords or tokens exposed | HTTP `403`/`200` | **PASS** |
