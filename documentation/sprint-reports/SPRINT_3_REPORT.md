# ReCircle — Sprint 3 Execution & Completion Report
## Dynamic Trust Score, Peer Reviews, Dispute Resolution, Admin Analytics & Audit History (`US16`–`US23`)

---

## 1. Sprint 3 Goal & User Stories

**Sprint 3 Goal**: Deliver the final vertical slice of ReCircle: the **Trust, Governance, Analytics, and Auditing Core** (`US16` through `US23`). This connects all 12 final frontend pages to the Spring Boot 3.2.5 + MySQL 9.5.0 backend with a mathematically verified 4-pillar trust score engine, peer review and rating integrity with duplicate prevention, dispute reporting and administrative resolution with penalties, database-backed governance KPIs and Recharts analytics, user account lifecycle management (suspension/reactivation), and an immutable administrative audit log.

| Story ID | User Story | Story Points | Priority | Status |
| :---: | :--- | :---: | :---: | :---: |
| **US16** | **Dynamic Trust Score**: Implement the 4-pillar trust model (15% Verified Account, 35% Completed Transactions, 30% User Reviews, 20% On-Time Returns, -15.00 dispute penalty) with neutral 50.0 baselines, HALF_UP rounding to 2 decimals, clamping [0, 100], and audit history. | 8 | High | **COMPLETED** |
| **US17** | **Peer Reviews & Ratings**: 1 to 5 star ratings, mandatory written feedback, duplicate review prevention for same transaction, participant-only authorization, review feed, and automatic trust score recalculation. | 5 | High | **COMPLETED** |
| **US18** | **Dispute Reporting**: Report a dispute against a transaction/item with category, description, and evidence note/URL, accessible to participants and administrators. | 5 | High | **COMPLETED** |
| **US19** | **Dispute Resolution**: Moderator/Admin resolves disputes with decision notes and optional -15.00 trust penalty; repeated resolution on resolved disputes is strictly blocked (`DISPUTE_ALREADY_RESOLVED`). | 5 | High | **COMPLETED** |
| **US20** | **Admin Dashboard**: Real-time database-backed aggregates (total users, verified users, item counts, active borrowings, active rentals, completed exchanges/donations, open disputes). | 5 | High | **COMPLETED** |
| **US21** | **Admin Analytics**: Multi-mode visualizations via Recharts (sharing type breakdown, monthly transaction trends, category distribution, user registration trends, trust score histogram) with date-range filters. | 8 | High | **COMPLETED** |
| **US22** | **Admin User Management**: Campus user directory with search/filters, trust score inspection, account suspension (`SUSPENDED`), and reactivation (`ACTIVE`) with admin-only authorization. | 5 | High | **COMPLETED** |
| **US23** | **Immutable Audit History**: Administrative audit trail recording actor ID, name, role, action type, target entity, target ID, details, and timestamp; sanitized to ensure no credentials or tokens are exposed. | 5 | High | **COMPLETED** |

---

## 2. Committed vs. Completed User Stories

- **Committed User Stories (Sprint 3)**: `8` (`US16`–`US23`, `46` Story Points)
- **Completed User Stories (Sprint 3)**: `8` (`US16`–`US23`, `46` Story Points — `100%` completion rate)
- **Total Project User Stories (Sprint 1 + Sprint 2 + Sprint 3)**: `23` User Stories (`US01`–`US23`, `129` Story Points completed)

---

## 3. Architecture & Functional Specification

### 3.1 Dynamic Trust Score Engine (`US16`)
- **Pillars & Weights:**
  - $w_1 = 0.15$: Campus Identity Verification (100.0 if email verified, 0.0 otherwise).
  - $w_2 = 0.35$: Completed Transactions ($\min(100.0, \frac{N_{\text{completed}}}{7} \times 100.0)$).
  - $w_3 = 0.30$: Peer Reviews & Ratings (Neutral baseline 50.0 if no reviews; $\frac{\overline{R}}{5.0} \times 100.0$ if reviews exist).
  - $w_4 = 0.20$: On-Time Return Track Record (Neutral baseline 50.0 if no returns; $\frac{N_{\text{on\_time}}}{N_{\text{returns}}} \times 100.0$ if returns exist).
- **Penalties & Boundaries:**
  - Penalty deduction: $-15.00$ points per confirmed dispute resolution with `penalizeReportedUser = true`.
  - Final Score: Clamped to $[0.00, 100.00]$ and rounded using `RoundingMode.HALF_UP` to 2 decimal places.
  - History: Every recalculation persists previous score, new score, delta, trigger event, and reason summary in `trust_score_history`.

### 3.2 Peer Review Integrity (`US17`)
- **Authorization & Duplication:**
  - Only participants (`initiator` or `counterparty`) of a `COMPLETED` transaction can review each other.
  - Submitting a second review on the same transaction by the same reviewer is blocked with `409 Conflict` (`DUPLICATE_REVIEW`).
  - Rating strictly validated to integer values $\in [1, 5]$.

### 3.3 Dispute Lifecycle & Governance (`US18`, `US19`)
- **State Machine:**
  - `OPEN -> UNDER_REVIEW -> RESOLVED` or `REJECTED`.
  - Evidence records (`DisputeEvidence`) store photo URLs and comments.
  - Resolution by Administrator or Moderator records resolution notes and triggers score recalculation if penalty applied.
  - Re-resolution of an already resolved dispute is blocked with `400 Bad Request` (`DISPUTE_ALREADY_RESOLVED`).

### 3.4 Governance Analytics & Audit Logging (`US20`–`US23`)
- **Database KPIs:** Dynamic counts queried directly from MySQL 9.5.0 repositories (`userRepository`, `itemRepository`, `transactionRecordRepository`, `disputeRepository`).
- **Interactive Recharts Datasets:** Dynamic monthly trend series, item category distribution, sharing type distribution, and trust score distribution buckets.
- **Administrative Trail:** Immutable audit logs (`AuditLog`) store actor details and sanitized actions (`USER_STATUS_UPDATED`, `DISPUTE_RESOLVED`, `ITEM_APPROVED`).

---

## 4. Frontend Route & Page Completion

All 12 Sprint 3 frontend pages and route aliases are wired and verified in React 19 + TypeScript + Vite:

1. **Trust Score Details** (`/trust-score`, `/users/:id/trust-score`) -> [`TrustScoreDetailsPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/profile/ProfileCommunityPages.tsx)
2. **Reviews Center** (`/reviews`, `/users/:id/reviews`) -> [`ReviewsPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/profile/ProfileCommunityPages.tsx)
3. **Dispute Filing** (`/disputes`, `/disputes/new`) -> [`DisputeReportingPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/profile/ProfileCommunityPages.tsx)
4. **My Disputes** (`/my-disputes`) -> [`DisputeReportingPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/profile/ProfileCommunityPages.tsx)
5. **Moderator Dashboard** (`/moderator`, `/moderator/dashboard`) -> [`ModeratorDashboardPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)
6. **Admin Command Center** (`/admin`, `/admin/dashboard`) -> [`AdminDashboardPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)
7. **Admin Analytics** (`/admin/analytics`) -> [`AdminAnalyticsPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)
8. **Admin User Directory** (`/admin/users`) -> [`AdminUsersPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)
9. **Admin Listing Governance** (`/admin/listings`, `/admin/items`) -> [`AdminListingsPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)
10. **Admin Transaction Ledger** (`/admin/transactions`) -> [`AdminTransactionsPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)
11. **Admin Dispute Management** (`/admin/disputes`, `/moderator/disputes`) -> [`AdminDisputesPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)
12. **Audit History Trail** (`/admin/audit-logs`) -> [`AdminAuditLogsPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/admin/AdminPages.tsx)

---

## 5. Summary of Automated Verification

- **Maven Clean Verify:** `30/30` Test Suites Passed (`0` Failures, `0` Errors, `0` Skipped).
- **Vite Build:** `2,422` modules transformed in `3.77s` without errors.
- **Production Artifact:** Executable JAR `recircle-backend-1.0.0.jar` created with embedded Tomcat and production profiles.
- **Screenshots:** All 26 system screenshots stored in [`recircle/screenshots/`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/).
