# ReCircle — Complete REST API Documentation (`/api/v1`)

All endpoints accept and return `application/json` (except `/api/v1/items/upload-image` which accepts `multipart/form-data`). Protected endpoints require the HTTP header:
```http
Authorization: Bearer <accessToken>
```

---

## 0. Health Check & System Readiness
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/v1/health` | Public | Returns service readiness status (`UP`), active JDK version (`21.0.6`), database connectivity status, and ISO-8601 timestamp. |

---

## 1. Authentication & Identity Endpoints (`/api/v1/auth`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register a new campus student account (`@srmist.edu.in`), hash password with BCrypt, initialize trust score, and issue JWT + time-limited single-use OTP. |
| `POST` | `/api/v1/auth/login` | Public | Authenticate with campus email and password; returns `accessToken`, `refreshToken`, and `UserSummaryDto`. |
| `POST` | `/api/v1/auth/logout` | Bearer | Revoke all active refresh tokens for the authenticated user. |
| `POST` | `/api/v1/auth/refresh` | Public | Exchange a valid non-revoked `refreshToken` for a new `accessToken`. |
| `POST` | `/api/v1/auth/verify-email` | Public/Bearer | Verify campus email using a single-use, non-expired 6-digit OTP and upgrade Verified Account trust pillar to `100.00`. |
| `POST` | `/api/v1/auth/forgot-password` | Public | Generate a time-limited single-use password reset token for a registered campus email. |
| `POST` | `/api/v1/auth/reset-password` | Public | Reset account password using a valid recovery token. |

---

## 2. User & Profile Endpoints (`/api/v1/users`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/v1/users/me` | Bearer | Retrieve current authenticated user's profile and trust summary. |
| `PUT` | `/api/v1/users/me` | Bearer | Update full name, department, year of study, contact number, hostel block, bio, avatar URL, and preferred pickup spot. |
| `GET` | `/api/v1/users/me/dashboard` | Bearer | Retrieve personalized student dashboard metrics, active borrows/lendings, upcoming returns, notifications, and recommended items. |
| `GET` | `/api/v1/users/{id}` | Public/Bearer | View public campus profile of user `{id}`. |
| `GET` | `/api/v1/users/{id}/reviews` | Public/Bearer | List peer reviews received by user `{id}`. |
| `GET` | `/api/v1/users/{id}/trust-score` | Public/Bearer | Retrieve detailed 4-pillar trust score breakdown and historical audit log for user `{id}`. |

---

## 3. Item, Category & Date-Based Availability Endpoints (`/api/v1/items`, `/api/v1/categories`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/v1/categories` | Public | List all 10 active campus resource categories. |
| `GET` | `/api/v1/items` | Public/Bearer | Paginated item discovery with `query`, `categoryId`, `department`, `sharingType`, `condition`, `availability`, `maxPrice`, and `sortBy` (`newest`, `oldest`, `rating`, `price_asc`, `trust_score`). |
| `GET` | `/api/v1/items/my-listings` | Bearer | Retrieve all non-archived listings owned by the current student. |
| `GET` | `/api/v1/items/{id}` | Public/Bearer | Retrieve full item details, gallery, owner verification, reviews, related items, and `reservedDateRanges`. |
| `GET` | `/api/v1/items/{id}/availability` | Public/Bearer | Derive date-range availability (`?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`) and list all active/approved booking windows (`reservedDateRanges`). |
| `POST` | `/api/v1/items` | Bearer | Create a new campus resource listing (`BORROW`, `RENT`, `EXCHANGE`, or `DONATE`). |
| `PUT` | `/api/v1/items/{id}` | Bearer (Owner/Admin) | Update an existing listing owned by the caller. |
| `DELETE` | `/api/v1/items/{id}` | Bearer (Owner/Admin/Mod) | Soft-archive a listing (`availabilityStatus = ARCHIVED`, `archived = true`) so it no longer appears in search results. |
| `POST` | `/api/v1/items/upload-image` | Bearer | Validate MIME type (`image/jpeg`, `image/png`, `image/webp`) and file size ($\le 5\text{ MB}$) and store item photograph. |

---

## 4. Resource Sharing Workflow Endpoints (`/api/v1/borrow-requests`, `/exchanges`, `/rentals`, `/donations`, `/transactions`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/v1/borrow-requests` | Bearer | Submit a borrow request for a date window (`startDate` to `endDate`); locks item row (`PESSIMISTIC_WRITE`) and validates no self-borrowing, no duplicate pending request (`DUPLICATE_BORROW_REQUEST`), and no overlapping reservations (`OVERLAPPING_RESERVATION`). |
| `GET` | `/api/v1/borrow-requests/my` | Bearer | List borrow requests initiated by the current student. |
| `GET` | `/api/v1/borrow-requests/received` | Bearer | List incoming borrow requests for items owned by the current student. |
| `GET` | `/api/v1/borrow-requests/{id}` | Bearer (Participant/Admin) | Retrieve full details of a specific borrow request (`BorrowRequestDetailsPage`). |
| `PUT` | `/api/v1/borrow-requests/{id}/approve` | Bearer (Lender) | Approve request for the requested date window, auto-reject conflicting `PENDING` requests for overlapping dates, notify borrowers, and keep listing `AVAILABLE` for non-overlapping dates. |
| `PUT` | `/api/v1/borrow-requests/{id}/reject` | Bearer (Lender) | Reject a pending borrow request with optional response note. |
| `PUT` | `/api/v1/borrow-requests/{id}/cancel` | Bearer (Participant) | Cancel a pending or approved borrow request before handover. |
| `PUT` | `/api/v1/borrow-requests/{id}/confirm-handover` | Bearer (Participant) | Confirm physical item handover and activate transaction (`ACTIVE`). |
| `PUT` | `/api/v1/borrow-requests/{id}/request-return` | Bearer (Borrower) | Mark borrowed resource as ready for return (`RETURN_REQUESTED`). |
| `PUT` | `/api/v1/borrow-requests/{id}/confirm-return` | Bearer (Lender) | Confirm item return, compute `returnedOnTime = !LocalDate.now().isAfter(endDate)`, record `returnedAt`, and recalculate trust scores. |
| `POST` | `/api/v1/exchanges` | Bearer | Propose an item-for-item exchange linking `requestedItemId` and `offeredItemId`; locks both item records in ascending ID order (`Math.min`, `Math.max`). |
| `GET` | `/api/v1/exchanges` | Bearer | List all sent and received exchange proposals for the current student. |
| `GET` | `/api/v1/exchanges/{id}` | Bearer (Participant/Admin) | Retrieve full details of an exchange offer (`ExchangeDetailsPage`). |
| `PUT` | `/api/v1/exchanges/{id}/accept` | Bearer (Receiver) | Lock both items in ascending ID order, accept exchange proposal, reserve both items, and atomically reject competing `PENDING` exchange offers. |
| `PUT` | `/api/v1/exchanges/{id}/reject` | Bearer (Participant) | Reject exchange proposal. |
| `PUT` | `/api/v1/exchanges/{id}/cancel` | Bearer (Participant) | Cancel an exchange offer before completion and restore item availability. |
| `PUT` | `/api/v1/exchanges/{id}/confirm-handover` | Bearer (Participant) | Record dual handover confirmation (`proposerHandoverConfirmed` / `receiverHandoverConfirmed`). Transitions to `HANDOVER_PENDING` after first party confirms, and automatically to `COMPLETED` once both parties confirm. |
| `PUT` | `/api/v1/exchanges/{id}/complete` | Bearer (Participant) | Complete exchange only when both parties have confirmed handover (`400 DUAL_HANDOVER_REQUIRED` if counterparty has not confirmed). |
| `POST` | `/api/v1/rentals/calculate` | Public/Bearer | Calculate backend rental quote (`rentalDays`, `dailyRate`, `totalRentalAmount`, `securityDeposit`, `totalPayableWithDeposit`, `availableForDates`, and payment disclaimer). |
| `GET` | `/api/v1/rentals/calculate` | Public/Bearer | Query-param alias (`?itemId=...&startDate=...&endDate=...`) for backend rental cost and availability calculation. |
| `POST` | `/api/v1/rentals` | Bearer | Create a rental booking; backend calculates `rentalDays` and `totalRentalAmount = rentalDays * dailyRate`; sets `paymentStatus = PENDING` unless demo simulation is requested. |
| `GET` | `/api/v1/rentals` | Bearer | List rentals where the current user is renter or owner. |
| `GET` | `/api/v1/rentals/{id}` | Bearer (Participant/Admin) | Retrieve rental agreement details (`RentalDetailsPage`). |
| `PUT` | `/api/v1/rentals/{id}/approve` | Bearer (Owner) | Approve rental booking for the date window without falsely marking payment as `PAID`, and auto-reject overlapping `PENDING` rental requests. |
| `PUT` | `/api/v1/rentals/{id}/reject` | Bearer (Owner) | Reject a pending rental booking. |
| `PUT` | `/api/v1/rentals/{id}/cancel` | Bearer (Participant) | Cancel a pending or approved rental booking before handover. |
| `PUT` | `/api/v1/rentals/{id}/confirm-handover` | Bearer (Participant) | Confirm rental handover (`ACTIVE`). Also mapped at `/api/v1/rentals/{id}/handover`. |
| `PUT` | `/api/v1/rentals/{id}/confirm-return` | Bearer (Owner) | Confirm rental return (`COMPLETED`), record on-time return status, and recalculate trust scores without fabricating deposit refund events. Also mapped at `/api/v1/rentals/{id}/complete`. |
| `POST` | `/api/v1/donations` | Bearer | Submit a motivation request for a `DONATE` listing (prevents duplicate active request by same student). |
| `GET` | `/api/v1/donations/requests` | Bearer | List donation requests submitted or received by the current user (also mapped at `/api/v1/donations`). |
| `GET` | `/api/v1/donations/requests/{id}` | Bearer (Participant/Admin) | Retrieve details of a specific donation request. |
| `PUT` | `/api/v1/donations/requests/{id}/approve` | Bearer (Donor) | Lock item row, approve recipient, enforce single-recipient rule, and atomically reject all competing `PENDING` donation requests for the item. |
| `PUT` | `/api/v1/donations/requests/{id}/reject` | Bearer (Donor) | Reject a pending donation request. |
| `PUT` | `/api/v1/donations/requests/{id}/cancel` | Bearer (Participant) | Cancel a pending or approved donation request before completion. |
| `PUT` | `/api/v1/donations/requests/{id}/complete` | Bearer (Participant) | Confirm donation handover completion (`COMPLETED`), mark item `DONATED`, and recalculate trust scores. |
| `GET` | `/api/v1/transactions` | Bearer | List unified transaction ledger entries (`BORROW`, `RENT`, `EXCHANGE`, `DONATE`) for the authenticated user without duplicate records. |
| `GET` | `/api/v1/transactions/{id}` | Bearer (Participant/Admin) | Retrieve specific transaction ledger record (`TransactionDetailsPage`). |

---

## 5. Community, Trust, Moderation & Admin Endpoints
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/v1/reviews` | Bearer | Submit a 1–5 star peer review for a `COMPLETED` transaction (one review per participant per transaction). |
| `GET` | `/api/v1/reviews/{id}` | Public/Bearer | Retrieve a specific review by ID. |
| `GET` | `/api/v1/notifications` | Bearer | Retrieve unread notification count and notification list. |
| `PUT` | `/api/v1/notifications/{id}/read` | Bearer | Mark a single notification as read. |
| `PUT` | `/api/v1/notifications/read-all` | Bearer | Mark all user notifications as read. |
| `POST` | `/api/v1/disputes` | Bearer | File a dispute or report an inappropriate listing/user. |
| `GET` | `/api/v1/disputes/my` | Bearer | List disputes filed by or involving the current student. |
| `GET` | `/api/v1/admin/disputes` | Admin/Mod | List all platform disputes for moderation review. |
| `PUT` | `/api/v1/admin/disputes/{id}/resolve` | Admin/Mod | Resolve or dismiss a dispute, record resolution notes, and optionally apply a `-15.00` trust score penalty (`penalizeReportedUser = true`). |
| `GET` | `/api/v1/admin/dashboard` | Admin/Mod | Retrieve all 11 live platform KPIs. |
| `GET` | `/api/v1/admin/analytics` | Admin/Mod | Retrieve all datasets for the 10 platform analytics charts. |
| `GET` | `/api/v1/admin/users` | Admin/Mod | Retrieve complete user directory with verification and trust metrics. |
| `PUT` | `/api/v1/admin/users/{id}/status` | Admin | Activate, verify, or suspend a user account and log audit event. |
| `GET` | `/api/v1/admin/items` | Admin/Mod | Retrieve all campus listings for administrative inspection. |
| `GET` | `/api/v1/admin/transactions` | Admin/Mod | Retrieve all campus transactions across all four workflows. |
| `GET` | `/api/v1/admin/audit-logs` | Admin/Mod | Retrieve immutable security and moderation audit logs. |
