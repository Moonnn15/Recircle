# ReCircle — Sprint 2 Implementation Audit Report
## Multi-Mode Resource Sharing Workflows (`US08`–`US15`)

**Audit Date:** November 14, 2026  
**Project Directory:** `C:\Users\monalisa\OneDrive\ドキュメント\Desktop\Dashboard\recircle\`  
**Backend Runtime & Compiler:** OpenJDK `21.0.6` (`C:\Program Files\Android\Android Studio\jbr`, `--release 21`)  
**Spring Boot Version:** `3.2.5` (Maven Wrapper `3.8.4`)  
**Database Engine:** MySQL Community Server `9.5.0` (`C:\Program Files\MySQL\MySQL Server 9.5\bin\mysqld.exe`, database `recircle_db`)  
**Frontend Toolchain:** React `19.2.3`, TypeScript `5.8.3`, Vite `8.1.3`, Tailwind CSS `4.2.0`

---

## 1. Executive Summary & Sprint 2 User Story Classification Matrix

All 8 Sprint 2 user stories (`US08`–`US15`) and all 12 Sprint 2 frontend pages have been implemented, integrated with the Spring Boot 3.2.5 + MySQL 9.5.0 backend, and verified via automated JUnit 5 integration tests (`24/24` passed) and live end-to-end execution through the Vite frontend proxy (`:5173 -> :8080 -> MySQL 9.5.0 :3307`).

| Story ID | User Story / Feature | Classification | Verification Evidence |
| :---: | :--- | :---: | :--- |
| **US08** | **Borrowing Requests** (date selection, duration limit check, duplicate pending block, overlapping reservation prevention, status tracking) | **PASS** | Verified via `TC12` (`test12_borrowLifecycleAndConflictPrevention`), `TC31` (`test19_sprint2BorrowRejectionUnauthorizedAndLateReturn`), `TC36` (`test24_sprint2ConcurrentRequestsForSameItem`), and live MySQL 9.5 E2E (`409 DUPLICATE_BORROW_REQUEST`, `409 OVERLAPPING_RESERVATION`). |
| **US09** | **Lending Management** (incoming requests, borrower trust score, approve/reject with decision notes, unauthorized approval prevention, notifications) | **PASS** | Verified via `TC12`, `TC31`, `TC36`, and live MySQL 9.5 E2E (`403 Forbidden` for non-owner, `200 OK` with `ownerResponseNote` for owner, auto-rejection of overlapping `PENDING` requests). |
| **US10** | **Handover & Return Management** (handover confirmation `ACTIVE`, return request `RETURN_REQUESTED`, return confirmation `COMPLETED`, `returnedAt`, `returnedOnTime`) | **PASS** | Verified via `TC12` (on-time return `returnedOnTime = true`), `TC31` (late return `returnedOnTime = false`), and live MySQL 9.5 E2E. |
| **US11** | **Exchange Marketplace** (propose swap with own item, lock both items in ascending ID order, accept/reject/cancel, dual handover confirmation required before completion) | **PASS** | Verified via `TC18` (`test13_exchangeWorkflow`) and `TC32` (`test20_sprint2ExchangeProposalAndCompetingRejection`) + live MySQL 9.5 E2E: unilateral `/complete` blocked with `400 DUAL_HANDOVER_REQUIRED`; first `/confirm-handover` -> `HANDOVER_PENDING`; second `/confirm-handover` -> `COMPLETED`. |
| **US12** | **Rental Management** (backend rental days & price calculation via `/api/v1/rentals/calculate`, approve/reject/cancel/handover/return, payment integrity preserved) | **PASS** | Verified via `TC19`, `TC28`, `TC33` (`test21_sprint2RentalQuoteHandoverAndCompletion`), and live MySQL 9.5 E2E: `paymentStatus` remains `PENDING` across approval, handover, and completion when no payment gateway is integrated. |
| **US13** | **Donation Management** (request donated item, approve/reject/cancel/complete, single-recipient enforcement, atomic rejection of competing `PENDING` requests) | **PASS** | Verified via `TC20`, `TC34` (`test22_sprint2MultipleDonationRequestsAtomicRejection`), and live MySQL 9.5 E2E: approving Request #1 atomically transitions competing Request #2 to `REJECTED` and updates its ledger record. |
| **US14** | **Unified Transaction History** (`BORROW`, `RENT`, `EXCHANGE`, `DONATE` synchronized without duplicate records, participant authorization check) | **PASS** | Verified via `TC35` (`test23_sprint2TransactionHistoryAndNotifications`) and live MySQL 9.5 E2E: all 4 sharing types present with unique `transactionCode`s. |
| **US15** | **Notifications** (created for requests, approvals, rejections, cancellations, handovers, returns, completions; unread count + individual & bulk mark-as-read) | **PASS** | Verified via `TC23`, `TC35`, and live MySQL 9.5 E2E (`GET /api/v1/notifications`, `PUT /api/v1/notifications/{id}/read`, `PUT /api/v1/notifications/read-all`). |

---

## 2. Backend Endpoints Added & Updated in Sprint 2

All endpoints are served under `/api/v1` by [`SharingWorkflowController.java`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/backend/src/main/java/com/recircle/controller/SharingWorkflowController.java) and [`SharingWorkflowService.java`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/backend/src/main/java/com/recircle/service/SharingWorkflowService.java):

### 2.1 Borrowing & Lending
- `POST /api/v1/borrow-requests` — Validates date range, max borrow duration, duplicate pending requests (`409 DUPLICATE_BORROW_REQUEST`), and overlapping reservations (`409 OVERLAPPING_RESERVATION`) using `PESSIMISTIC_WRITE` item row lock.
- `GET /api/v1/borrow-requests/my` — Outgoing borrow requests for current user.
- `GET /api/v1/borrow-requests/received` — Incoming lending requests for current item owner.
- `GET /api/v1/borrow-requests/{id}` *(Added in Sprint 2)* — Participant/admin-protected borrow request detail lookup.
- `PUT /api/v1/borrow-requests/{id}/approve` — Locks item row, approves request, auto-rejects overlapping `PENDING` requests with borrower notifications, and keeps item `AVAILABLE` for non-overlapping dates.
- `PUT /api/v1/borrow-requests/{id}/reject` — Rejects a pending borrow request with optional owner note.
- `PUT /api/v1/borrow-requests/{id}/cancel` — Cancels a pending or approved borrow request before handover.
- `PUT /api/v1/borrow-requests/{id}/confirm-handover` — Transitions `APPROVED -> ACTIVE` and records `handoverConfirmedAt`.
- `PUT /api/v1/borrow-requests/{id}/request-return` — Transitions `ACTIVE -> RETURN_REQUESTED` and records `returnRequestedAt`.
- `PUT /api/v1/borrow-requests/{id}/confirm-return` — Transitions `ACTIVE`/`RETURN_REQUESTED -> COMPLETED`, records `returnedAt`, computes `returnedOnTime = !LocalDate.now().isAfter(endDate)`, and recalculates trust scores.

### 2.2 Peer Exchanges
- `POST /api/v1/exchanges` — Locks both `requestedItemId` and `offeredItemId` in **ascending ID order** (`Math.min`, `Math.max`) via `itemRepository.findByIdForUpdate` and creates a `PENDING` exchange proposal.
- `GET /api/v1/exchanges` — Lists sent and received exchange proposals for the authenticated user.
- `GET /api/v1/exchanges/{id}` — Participant/admin-protected exchange detail lookup.
- `PUT /api/v1/exchanges/{id}/accept` — Locks both items in ascending ID order, reserves both items (`RESERVED`), transitions offer to `ACCEPTED`, and atomically rejects competing `PENDING` exchange offers involving either item.
- `PUT /api/v1/exchanges/{id}/reject` — Rejects a pending or accepted exchange offer and restores item availability.
- `PUT /api/v1/exchanges/{id}/cancel` *(Added in Sprint 2)* — Cancels a pending or accepted exchange offer before completion and restores item availability.
- `PUT /api/v1/exchanges/{id}/confirm-handover` — Records dual handover confirmation (`proposerHandoverConfirmed` / `receiverHandoverConfirmed`); transitions to `HANDOVER_PENDING` after the first party confirms and automatically to `COMPLETED` once both parties have confirmed.
- `PUT /api/v1/exchanges/{id}/complete` — Enforces dual handover confirmation (`400 DUAL_HANDOVER_REQUIRED` if counterparty has not confirmed).

### 2.3 Rentals
- `POST /api/v1/rentals/calculate` & `GET /api/v1/rentals/calculate` *(Added in Sprint 2)* — Backend calculation of `rentalDays`, `dailyRate`, `totalRentalAmount`, `securityDeposit`, `totalPayableWithDeposit`, `availableForDates`, and transparent payment disclaimer.
- `POST /api/v1/rentals` — Creates rental request with backend-computed pricing and `paymentStatus = PENDING` (unless demo simulation is explicitly requested).
- `GET /api/v1/rentals` — Lists user rentals (as renter or owner).
- `GET /api/v1/rentals/{id}` — Participant/admin-protected rental detail lookup.
- `PUT /api/v1/rentals/{id}/approve` — Locks item row, approves rental for the date window without falsely marking `paymentStatus = PAID`, and auto-rejects overlapping `PENDING` rentals.
- `PUT /api/v1/rentals/{id}/reject` — Rejects a pending rental request.
- `PUT /api/v1/rentals/{id}/cancel` *(Added in Sprint 2)* — Cancels a pending or approved rental before handover.
- `PUT /api/v1/rentals/{id}/confirm-handover` (alias `/handover`) — Transitions `APPROVED -> ACTIVE`.
- `PUT /api/v1/rentals/{id}/confirm-return` (alias `/complete`) — Transitions `ACTIVE -> COMPLETED`, computes `returnedOnTime`, and preserves payment integrity (`paymentStatus` stays `PENDING`).

### 2.4 Donations
- `POST /api/v1/donations` & `POST /api/v1/donations/{id}/request` — Submits a donation request and blocks duplicate active requests by the same student (`409 DUPLICATE_DONATION_REQUEST`).
- `GET /api/v1/donations/requests` (alias `/api/v1/donations`) — Lists sent and received donation requests.
- `GET /api/v1/donations/requests/{id}` *(Added in Sprint 2)* — Participant/admin-protected donation request detail lookup.
- `PUT /api/v1/donations/requests/{id}/approve` — Locks item row, approves recipient, marks item `RESERVED`, and **atomically rejects all competing `PENDING` donation requests** (`findCompetingPendingDonations`) while updating their unified transaction ledger records and notifying requesters.
- `PUT /api/v1/donations/requests/{id}/reject` — Rejects a pending donation request.
- `PUT /api/v1/donations/requests/{id}/cancel` *(Added in Sprint 2)* — Cancels a pending or approved donation request before handover completion.
- `PUT /api/v1/donations/requests/{id}/complete` — Completes donation handover (`COMPLETED`) and marks item `DONATED`.

### 2.5 Unified Transactions & Notifications
- `GET /api/v1/transactions` — Returns deduplicated unified transaction history (`BORROW`, `RENT`, `EXCHANGE`, `DONATE`) for the authenticated user.
- `GET /api/v1/transactions/{id}` — Participant/admin-protected transaction detail lookup.
- `GET /api/v1/notifications` — Returns `unreadCount` and ordered notification list.
- `PUT /api/v1/notifications/{id}/read` — Marks an individual notification as read.
- `PUT /api/v1/notifications/read-all` — Marks all user notifications as read.

---

## 3. Frontend Pages Added & Updated in Sprint 2

All 12 required Sprint 2 pages are implemented in [`SharingPages.tsx`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/sharing/SharingPages.tsx) and [`ProfileCommunityPages.tsx`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/profile/ProfileCommunityPages.tsx) and registered in [`App.tsx`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/App.tsx):

| # | Page Name | Route(s) | Key UI Features |
| :---: | :--- | :--- | :--- |
| 1 | **Borrowing Page** | `/borrowing` | Status filter tabs, `WorkflowTimeline`, confirmation modals for Handover, Request Return, and Cancel, plus link to Borrow Details. |
| 2 | **Lending Page** | `/lending` | Incoming requests queue with borrower trust score badge, decision-note modal for Approve/Reject, Handover, and Confirm Return (`returnedOnTime` badge). |
| 3 | **Borrow Request Details Page** | `/borrowing/:id`, `/borrow-requests/:id` | Dedicated detail card with `WorkflowTimeline`, pickup location, owner decision note, handover/return timestamps, and role-based action buttons. |
| 4 | **Exchange Marketplace Page** | `/exchanges` | Modal to propose an exchange using one of the student's own `AVAILABLE` listings, status filter tabs, and Sent/Received proposals list. |
| 5 | **Exchange Details Page** | `/exchanges/:id` | Side-by-side comparison of Offered vs. Requested items, dual handover confirmation badges (`proposerHandoverConfirmed` & `receiverHandoverConfirmed`), and Accept/Reject/Cancel/Confirm Handover actions. |
| 6 | **Rental Management Page** | `/rentals` | Renter & Owner tabs, backend-calculated days/daily rate/deposit/total breakdown, transparent payment disclaimer banner (`PENDING` vs `PAID_SIMULATED`), and workflow actions. |
| 7 | **Rental Details Page** | `/rentals/:id` | Detailed rental financial breakdown, payment integrity disclaimer, `WorkflowTimeline`, handover/return timestamps, and role-based actions (`Confirm Rental Return` without misleading deposit refund claims). |
| 8 | **Donation Marketplace Page** | `/donations` | Browse available campus donations, submit motivation modal, and link to Donation Requests queue. |
| 9 | **Donation Requests Page** | `/donations/requests`, `/donation-requests` | Donor incoming requests & Student outgoing requests with motivation messages, `WorkflowTimeline`, Approve (with atomic competing-request rejection warning), Reject, Cancel, and Complete Handover actions. |
| 10 | **Transaction History Page** | `/transactions` | Unified ledger filterable by sharing mode (`ALL`, `BORROW`, `RENT`, `EXCHANGE`, `DONATE`) and status (`ALL`, `PENDING`, `ACTIVE`, `COMPLETED`, `REJECTED`, `CANCELLED`). |
| 11 | **Transaction Details Page** | `/transactions/:id` | Full audit record displaying `transactionCode`, sharing mode, primary/secondary items, initiator & owner profiles, dates, payment status, and deep link to the underlying workflow record or peer review page. |
| 12 | **Notifications Page** | `/notifications` | Unread count badge, `All` / `Unread` filter tabs, loading/error/empty states, deep-link `Open` button (`targetPath`), individual `Mark Read`, and `Mark All as Read`. |

Additionally, [`ItemDetailsPage`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/frontend/src/pages/items/ItemPages.tsx) dynamically queries `GET /api/v1/items/{id}/availability` and `POST /api/v1/rentals/calculate` whenever the user selects `startDate` and `endDate`, displaying real-time date conflict warnings and backend-computed rental totals before submission.

---

## 4. Database & Repository Changes in Sprint 2

No destructive DDL changes were required to the 21-table MySQL 9.5 schema (`database/schema.sql`), as all Sprint 2 columns (`proposer_handover_confirmed`, `receiver_handover_confirmed`, `returned_on_time`, `handover_confirmed_at`, `return_requested_at`, `returned_at`, `version`, etc.) were already normalized and verified in Phase 0. The following JPA repository conflict-detection queries were added:
- [`BorrowRequestRepository.java`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/backend/src/main/java/com/recircle/repository/BorrowRequestRepository.java): Added `existsDuplicatePendingBorrow(itemId, borrowerId, startDate, endDate)`.
- [`ExchangeOfferRepository.java`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/backend/src/main/java/com/recircle/repository/ExchangeOfferRepository.java): Added `findCompetingPendingExchanges(item1Id, item2Id, excludeId)`.
- [`DonationRequestRepository.java`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/backend/src/main/java/com/recircle/repository/DonationRequestRepository.java): Added `existsByItemIdAndRequesterIdAndStatusIn(...)` and `findCompetingPendingDonations(itemId, excludeId)`.

---

## 5. Build & Automated Test Verification Results

### 5.1 Backend Maven Verification (`mvnw.cmd clean verify` on JDK 21.0.6)
```text
[INFO] Compiling 79 source files with javac [debug release 21] to target\classes
[INFO] Compiling 2 source files with javac [debug release 21] to target\test-classes
[INFO] Running com.recircle.ReCircleBackendIntegrationTest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 49.46 s -- in com.recircle.ReCircleBackendIntegrationTest
[INFO] Building jar: C:\Users\monalisa\OneDrive\ドキュメント\Desktop\Dashboard\recircle\backend\target\recircle-backend-1.0.0.jar
[INFO] BUILD SUCCESS
```

### 5.2 Complete Output of All 24/24 JUnit 5 Integration Tests
```text
[PASS] TC01: Valid Student Registration with SRM Campus Email
[PASS] TC02: Invalid Non-Campus Email Rejection
[PASS] TC03: Duplicate Registration Prevention
[PASS] TC04: Weak Password Validation
[PASS] TC05: Valid Login for Seeded Student & Admin
[PASS] TC06: Invalid Login Credentials Rejected
[PASS] TC07: Unauthorized Request Without JWT Blocked
[PASS] TC08: Role-Based Access Control (Student Blocked from Admin Endpoints)
[PASS] TC09: Item Creation Across Borrow, Rent, and Donate
[PASS] TC10: Item Editing by Owner & Unauthorized Edit Prevention
[PASS] TC11 & TC12: Search, Filtering, and Item Archival
[PASS] TC13, TC14, TC15, TC16, TC17, TC29: Borrow Request Lifecycle & Conflict Prevention
[PASS] TC18: Exchange Proposal, Acceptance & Dual Handover Completion
[PASS] TC19: Backend Rental Cost Calculation (4 Days * ₹50 = ₹200)
[PASS] TC20: Donation Request Approval & Single-Recipient Enforcement
[PASS] TC21 & TC22: Review Eligibility, Duplicate Review Prevention & Trust Score Recalculation
[PASS] TC23, TC24, TC25, TC26, TC27: Notifications, Disputes, Image Upload Validation & Admin Analytics
[PASS] TC28: Health Check, Date-Range Availability, Payment Integrity (PENDING on Approval), Single-Use OTP & Dispute Penalty
[PASS] TC31 (Sprint 2): Borrow Rejection, Unauthorized Approvals, Details Endpoint & Late Return Calculation
[PASS] TC32 (Sprint 2): Exchange Proposal Creation, Competing Offer Rejection & Cancellation
[PASS] TC33 (Sprint 2): Rental Quote Calculation Endpoint, Handover, Completion & Payment Integrity
[PASS] TC34 (Sprint 2): Multiple Donation Requests Atomic Rejection & Single-Recipient Enforcement
[PASS] TC35 (Sprint 2): Unified Transaction History & Individual Notification Read Tracking
[PASS] TC36 (Sprint 2): Multi-Threaded Concurrent Approval Protection for Overlapping Item Requests
=================================================================
ALL JUNIT 5 INTEGRATION TESTS PASSED: 24/24
=================================================================
```

### 5.3 Frontend Production Build (`npm run build`)
```text
> recircle-frontend@1.0.0 build
> vite build

vite v8.1.3 building client environment for production...
✓ 2422 modules transformed.
dist/index.html                   0.91 kB │ gzip:   0.53 kB
dist/assets/index-Bg9bNjji.css   42.56 kB │ gzip:   8.10 kB
dist/assets/index-vSjIlwhZ.js   915.85 kB │ gzip: 238.51 kB
✓ built in 3.15s
```

### 5.4 Live End-to-End Workflow Verification on MySQL Server 9.5.0
Executed against running **MySQL Server 9.5.0** (`127.0.0.1:3307`), Spring Boot Backend on **OpenJDK 21.0.6** (`127.0.0.1:8080`), and Vite Dev Server proxy (`127.0.0.1:5173` -> `:8080`):
```text
=== STEP 1: STARTING MYSQL 9.5.0 SERVER (PORT 3307) WITH SCHEMA + SEED ===
=== STEP 2: STARTING SPRING BOOT BACKEND ON JDK 21 (PORT 8080 -> MYSQL 9.5.0:3307) ===
=== STEP 3: STARTING VITE FRONTEND DEV SERVER (PORT 5173 -> PROXY :8080) ===
[PASS] Logged in Aarav (u1), Diya (u2), and Rohan (u3) against MySQL 9.5.0
[PASS] US08/US09/US10: Borrow #3 PENDING->APPROVED->ACTIVE->RETURN_REQUESTED->COMPLETED (returnedOnTime=True), duplicate & overlap blocked (409), unauthorized approval blocked (403)
[PASS] US11: Exchange #1 ACCEPTED -> unilateral complete blocked (400 DUAL_HANDOVER_REQUIRED) -> HANDOVER_PENDING -> COMPLETED
[PASS] US12: Rental #2 quote calculated (4 days, total=200.0), APPROVED->ACTIVE->COMPLETED with paymentStatus='PENDING'
[PASS] US13: Donation #2 APPROVED & COMPLETED; competing Donation #3 atomically transitioned to 'REJECTED'
[PASS] US14: Unified Transaction Ledger returned 6 deduplicated records covering ['BORROW', 'DONATE', 'EXCHANGE', 'RENT']
[PASS] US15: Notifications Center returned 8 notifications (unreadCount=8); marked #10 read
[PASS] Saved 12 Sprint 2 screenshots in recircle/screenshots/
=================================================================
ALL SPRINT 2 LIVE MYSQL 9.5.0 E2E WORKFLOW VERIFICATIONS PASSED!
=================================================================
```

### 5.5 Verified Sprint 2 UI Screenshots (`recircle/screenshots/`)
The following high-resolution UI screenshots of the live running application were captured:
1. [`08_borrow_management.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/08_borrow_management.png) — My Borrowing Requests (`/borrowing`) with status tabs, lifecycle badges, and return actions.
2. [`08b_lending_management.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/08b_lending_management.png) — Incoming Lending Requests (`/lending`) with borrower trust scores, owner approval/rejection dialogs, and return confirmation.
3. [`08c_borrow_details.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/08c_borrow_details.png) — Borrow Request Details (`/borrowing/:id`) showing step-by-step `WorkflowTimeline`, dates, and timestamps.
4. [`09_exchange_marketplace.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/09_exchange_marketplace.png) — Exchange Marketplace (`/exchanges`) with item proposal modal, status filters, and proposal cards.
5. [`09b_exchange_details.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/09b_exchange_details.png) — Exchange Details (`/exchanges/:id`) with side-by-side offered/requested items and dual handover confirmation verification.
6. [`10_rental_management.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/10_rental_management.png) — Campus Rental Management (`/rentals`) with backend calculation, transparent payment disclaimers, and owner/renter tabs.
7. [`10b_rental_details.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/10b_rental_details.png) — Rental Agreement Details (`/rentals/:id`) showing item, daily rate, security deposit, duration, and status.
8. [`11_donation_hub.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/11_donation_hub.png) — Campus Donation Hub (`/donations`) displaying available free resources and request modal.
9. [`11b_donation_requests.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/11b_donation_requests.png) — Donation Requests Queue (`/donations/requests`) showing donor approval with atomic competing-request rejection.
10. [`14_transaction_history.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/14_transaction_history.png) — Unified Transaction Ledger (`/transactions`) with filters across `BORROW`, `RENT`, `EXCHANGE`, `DONATE`.
11. [`14b_transaction_details.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/14b_transaction_details.png) — Transaction Record Details (`/transactions/:id`) with full audit timestamps, participants, and status.
12. [`15_notifications_center.png`](file:///c:/Users/monalisa/OneDrive/ドキュメント/Desktop/Dashboard/recircle/screenshots/15_notifications_center.png) — In-App Notifications Center (`/notifications`) with real-time workflow alerts, unread counter, and mark-as-read buttons.

---

## 6. Known Limitations
1. **No External Payment Gateway**: In compliance with Section 2 (`US12` / Rule `3.B`), rental payments default to `PENDING` (or `PAID_SIMULATED` only when the student explicitly checks the academic demo simulation toggle), and the system never fabricates live payment captures or deposit refunds.
2. **Polling / Action-Driven Notifications**: In-app notifications are persisted in MySQL 9.5 (`notifications` table) and refreshed automatically upon login, route transitions, and workflow actions rather than via persistent WebSockets.
