# ReCircle — Sprint 2 Execution & Completion Report
## Multi-Mode Resource Sharing Workflows (`US08`–`US15`)

---

## 1. Sprint 2 Goal & User Stories

**Sprint 2 Goal**: Deliver the complete **Multi-Mode Resource Sharing Workflows** vertical slice (`US08` through `US15`), connecting all 12 Sprint 2 React + TypeScript frontend pages to the Spring Boot 3.2.5 + MySQL 9.5.0 backend with row-level pessimistic locking (`SELECT ... FOR UPDATE`), ascending-ID deadlock prevention, dual handover confirmation, backend-computed rental pricing, single-recipient donation enforcement, unified transaction history, and real-time in-app notifications.

| Story ID | User Story | Story Points | Priority | Status |
| :---: | :--- | :---: | :---: | :---: |
| **US08** | **Borrowing Requests**: As a student, I want to request to borrow resources for a selected start and end date with automatic availability and duration validation, duplicate-pending prevention, and overlapping reservation prevention. | 8 | High | **COMPLETED** |
| **US09** | **Lending Management**: As a resource owner, I want to view incoming borrowing requests with borrower trust scores, approve or reject requests with decision notes, auto-reject overlapping pending requests, and notify borrowers. | 5 | High | **COMPLETED** |
| **US10** | **Handover & Return Management**: As a borrower/lender, I want to confirm physical handover (`ACTIVE`), request item return (`RETURN_REQUESTED`), confirm return (`COMPLETED`), record `returnedAt`, and automatically compute `returnedOnTime`. | 5 | High | **COMPLETED** |
| **US11** | **Exchange Marketplace**: As a student, I want to propose an item-for-item exchange using one of my own listed resources, lock both items in ascending ID order (`Math.min`, `Math.max`), accept/reject/cancel proposals, and require dual handover confirmation from both parties before completion. | 8 | High | **COMPLETED** |
| **US12** | **Rental Management**: As a student, I want backend-calculated rental days, daily rate, security deposit, and total payable (`POST /api/v1/rentals/calculate`), and as an owner/renter I want to approve, reject, cancel, confirm handover, and confirm return without fabricating unverified payment or deposit refund events. | 8 | High | **COMPLETED** |
| **US13** | **Donation Management**: As a student, I want to request donated campus resources with a motivation note, and as a donor I want to approve one recipient while atomically rejecting all competing `PENDING` requests for that item. | 5 | High | **COMPLETED** |
| **US14** | **Unified Transaction History**: As a student, I want a unified, deduplicated transaction ledger across `BORROW`, `RENT`, `EXCHANGE`, and `DONATE` showing transaction code, item(s), counterparty, dates, and status. | 5 | High | **COMPLETED** |
| **US15** | **In-App Notifications**: As a student, I want real-time notifications created for requests, approvals, rejections, cancellations, handovers, returns, and completions, with unread counts and individual/bulk mark-as-read actions. | 5 | High | **COMPLETED** |

---

## 2. Committed vs. Completed User Stories
- **Committed User Stories (Sprint 2)**: `8` (`US08`–`US15`, `49` Story Points)
- **Completed User Stories (Sprint 2)**: `8` (`US08`–`US15`, `49` Story Points — `100%` completion rate)
- **Cumulative Velocity (Sprint 1 + Sprint 2)**: `15` User Stories (`US01`–`US15`, `83` Story Points completed)

---

## 3. Functional Specification & Enforced State Transitions

### 3.1 Borrowing & Lending Workflow (`US08`, `US09`, `US10`)
- **Valid State Transitions**:
  - `PENDING -> APPROVED` (Lender only; locks item row via `PESSIMISTIC_WRITE`, verifies no overlapping `APPROVED`/`ACTIVE`/`RETURN_REQUESTED` borrow or rental exists, and atomically transitions overlapping `PENDING` requests to `REJECTED`).
  - `PENDING -> REJECTED` (Lender only).
  - `PENDING -> CANCELLED` or `APPROVED -> CANCELLED` (Borrower or Lender before handover).
  - `APPROVED -> ACTIVE` (Handover confirmed; records `handoverConfirmedAt`).
  - `ACTIVE -> RETURN_REQUESTED` (Borrower initiates return; records `returnRequestedAt`).
  - `ACTIVE -> COMPLETED` or `RETURN_REQUESTED -> COMPLETED` (Lender confirms return; records `returnedAt` and computes `returnedOnTime = !LocalDate.now().isAfter(endDate)`).

### 3.2 Peer Exchange Workflow (`US11`)
- **Ascending-ID Lock Order**: Both `createExchangeOffer` and `acceptExchangeOffer` acquire `PESSIMISTIC_WRITE` locks on `Math.min(requestedItemId, offeredItemId)` followed by `Math.max(requestedItemId, offeredItemId)` to prevent AB-BA deadlocks when two students propose swaps involving the same items concurrently.
- **Dual Handover Confirmation**:
  - `PENDING -> ACCEPTED` (Receiver accepts; both items transition to `RESERVED`, and competing `PENDING` exchange offers involving either item are atomically rejected).
  - `ACCEPTED -> HANDOVER_PENDING` (First participant confirms physical handover via `PUT /api/v1/exchanges/{id}/confirm-handover`).
  - `HANDOVER_PENDING -> COMPLETED` (Second participant confirms handover; both `proposerHandoverConfirmed` and `receiverHandoverConfirmed` are `true`, both items transition to `EXCHANGED`, and trust scores are recalculated).
  - Calling `PUT /api/v1/exchanges/{id}/complete` before both participants have confirmed handover is rejected with `400 Bad Request` (`DUAL_HANDOVER_REQUIRED`).

### 3.3 Rental Management Workflow (`US12`)
- **Backend Price & Availability Calculation**:
  - `POST /api/v1/rentals/calculate` and `GET /api/v1/rentals/calculate` compute `rentalDays = max(1, ChronoUnit.DAYS.between(start, end))`, `totalRentalAmount = dailyRate * rentalDays`, `totalPayableWithDeposit = totalRentalAmount + securityDeposit`, and `availableForDates` on the backend.
- **Payment & Deposit Integrity**:
  - Approving a rental (`PUT /api/v1/rentals/{id}/approve`), confirming handover (`ACTIVE`), or confirming return (`COMPLETED`) never fabricates `paymentStatus = PAID` or claims a deposit was refunded without a verified payment gateway event.

### 3.4 Donation Management Workflow (`US13`)
- **Single-Recipient Atomic Enforcement**:
  - `PENDING -> APPROVED` (Donor locks item row via `PESSIMISTIC_WRITE`, marks item `RESERVED`, and atomically transitions all competing `PENDING` donation requests for that item to `REJECTED` while updating their unified transaction ledger records and notifying requesters).
  - `APPROVED -> COMPLETED` (Handover confirmed; item transitions to `DONATED`).

---

## 4. Sprint 2 Daily Progress Log

| Day | Focus Area | Completed Engineering Tasks | Blockers & Resolution |
| :---: | :--- | :--- | :--- |
| **Day 1** | Borrowing, Lending & Concurrency (`US08`–`US10`) | Added `GET /api/v1/borrow-requests/{id}`, duplicate pending borrow detection (`DUPLICATE_BORROW_REQUEST`), auto-rejection notifications, and late return calculation (`returnedOnTime`). | Verified multi-threaded concurrent approval serialization (`TC36`). |
| **Day 2** | Peer Exchange Ascending Lock & Dual Handover (`US11`) | Upgraded `createExchangeOffer` and `acceptExchangeOffer` with ascending-ID `PESSIMISTIC_WRITE` locks, competing offer auto-rejection, cancellation endpoint (`PUT /api/v1/exchanges/{id}/cancel`), and strict dual-handover enforcement (`DUAL_HANDOVER_REQUIRED`). | Updated `TC18` and added `TC32` to verify unilateral completion is blocked until both parties confirm handover. |
| **Day 3** | Rental Calculation & Donation Atomic Rejection (`US12`–`US13`) | Implemented `POST/GET /api/v1/rentals/calculate`, `PUT /api/v1/rentals/{id}/cancel`, `GET /api/v1/donations/requests/{id}`, `PUT /api/v1/donations/requests/{id}/cancel`, and atomic rejection of competing donation requests (`findCompetingPendingDonations`). | Verified `paymentStatus` remains `PENDING` across rental approval, handover, and completion when no payment gateway is integrated (`TC33`). |
| **Day 4** | 12 Sprint 2 Frontend Pages & Live Date Availability (`US08`–`US15`) | Built and wired `/borrowing`, `/lending`, `/borrowing/:id`, `/exchanges`, `/exchanges/:id`, `/rentals`, `/rentals/:id`, `/donations`, `/donations/requests`, `/transactions`, `/transactions/:id`, and `/notifications` with `WorkflowTimeline`, `ConfirmActionModal`, status filter tabs, loading/empty/error states, and live date availability + rental quote previews on `ItemDetailsPage`. | Verified `npm run build` (`vite build`) succeeds with `0` errors. |
| **Day 5** | JUnit 5 Suite (`24/24`), MySQL 9.5 E2E & Screenshots | Expanded `ReCircleBackendIntegrationTest` to `24` test methods (`TC01`–`TC36`), verified against both H2 and live MySQL 9.5.0, captured live browser screenshots of all Sprint 2 pages, and authored `SPRINT_2_AUDIT.md`. | All `24/24` JUnit test suites and live MySQL 9.5 E2E workflows passed. |

---

## 5. Sprint 2 Retrospective
- **What Went Well**:
  - Ascending-ID pessimistic row locking (`Math.min(id1, id2)` then `Math.max(id1, id2)`) in `SharingWorkflowService` prevented cross-item deadlocks during exchange proposals and acceptances.
  - Centralizing ledger updates via `createOrUpdateLedgerRecord` and `updateLedgerStatus` guaranteed 1-to-1 synchronization between domain workflow tables (`borrow_requests`, `exchange_offers`, `rentals`, `donation_requests`) and the unified `transactions` table without duplicate records.
  - Real-time date-range availability checks and backend rental quote calculations on `ItemDetailsPage` prevented invalid booking submissions before form submission.
- **What Could Be Improved**:
  - Adding WebSocket/SSE push in a future enhancement would eliminate the need for manual refresh on the Notifications page, though automatic refresh on route navigation and workflow actions works reliably.
- **Ready for Sprint 3**:
  - All 4 resource-sharing workflows now produce `COMPLETED` transactions in MySQL 9.5, providing the exact verified foundation required for Sprint 3 (Peer Reviews, Dynamic 4-Pillar Trust Score Engine, Dispute Resolution, and Admin/Moderator Governance).
