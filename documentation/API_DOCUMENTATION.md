# ReCircle — Complete REST API Documentation (`/api/v1`)

All endpoints accept and return `application/json` (except `/api/v1/items/upload-image` which accepts `multipart/form-data`). Protected endpoints require the HTTP header:
```http
Authorization: Bearer <accessToken>
```

---

## 1. Authentication Endpoints (`/api/v1/auth`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register a new campus student account (`@srmist.edu.in`), hash password with BCrypt, initialize trust score, and issue JWT + OTP. |
| `POST` | `/api/v1/auth/login` | Public | Authenticate with campus email and password; returns `accessToken`, `refreshToken`, and `UserSummaryDto`. |
| `POST` | `/api/v1/auth/logout` | Bearer | Revoke active refresh tokens and record security audit log. |
| `POST` | `/api/v1/auth/refresh-token` | Public | Exchange a valid `refreshToken` for a new `accessToken`. |
| `POST` | `/api/v1/auth/verify-email` | Bearer | Verify campus email using 6-digit OTP and upgrade Verified Account trust pillar to `100.0`. |
| `POST` | `/api/v1/auth/forgot-password` | Public | Generate a time-limited password reset token for a registered campus email. |
| `POST` | `/api/v1/auth/reset-password` | Public | Reset account password using a valid recovery token. |

---

## 2. User & Profile Endpoints (`/api/v1/users`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/v1/users/me` | Bearer | Retrieve current authenticated user's profile and trust summary. |
| `PUT` | `/api/v1/users/me` | Bearer | Update full name, department, year of study, contact number, hostel block, bio, avatar URL, and preferred pickup spot. |
| `PUT` | `/api/v1/users/me/password` | Bearer | Change password after verifying current password. |
| `GET` | `/api/v1/users/me/dashboard` | Bearer | Retrieve personalized student dashboard metrics, active borrows/lendings, upcoming returns, notifications, and recommended items. |
| `GET` | `/api/v1/users/{id}` | Bearer | View public campus profile of another user. |
| `GET` | `/api/v1/users/{id}/items` | Bearer | List active item listings published by user `{id}`. |
| `GET` | `/api/v1/users/{id}/reviews` | Bearer | List peer reviews received by user `{id}`. |
| `GET` | `/api/v1/users/{id}/trust-score` | Bearer | Retrieve detailed 4-pillar trust score breakdown and historical audit log for user `{id}`. |

---

## 3. Item & Category Endpoints (`/api/v1/items`, `/api/v1/categories`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `GET` | `/api/v1/items` | Public/Bearer | Paginated item discovery with query, category, department, sharingType, itemCondition, availability, maxPrice, and sort (`newest`, `oldest`, `highest_rated`, `lowest_price`, `trust_score`). |
| `GET` | `/api/v1/items/my-listings` | Bearer | Retrieve all non-archived listings owned by the current student. |
| `GET` | `/api/v1/items/{id}` | Public/Bearer | Retrieve full item details, gallery, owner verification, reviews, and related items. |
| `POST` | `/api/v1/items` | Bearer | Create a new campus resource listing (`BORROW`, `RENT`, `EXCHANGE`, or `DONATE`). |
| `PUT` | `/api/v1/items/{id}` | Bearer (Owner/Admin) | Update an existing listing owned by the caller. |
| `DELETE` | `/api/v1/items/{id}` | Bearer (Owner/Admin) | Soft-archive a listing (`availabilityStatus = ARCHIVED`) so it no longer appears in search results. |
| `POST` | `/api/v1/items/upload-image` | Bearer | Validate MIME type (`image/jpeg`, `image/png`, `image/webp`) and file size ($\le 5\text{ MB}$) and store item photograph. |
| `GET` | `/api/v1/categories` | Public | List all active campus resource categories. |
| `POST` | `/api/v1/categories` | Admin | Create a new resource category. |
| `PUT` | `/api/v1/categories/{id}` | Admin | Update an existing category's name, description, or active status. |

---

## 4. Resource Sharing Workflow Endpoints (`/api/v1/borrow-requests`, `/exchanges`, `/rentals`, `/donations`, `/transactions`)
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/v1/borrow-requests` | Bearer | Submit a borrow request for a date window (`startDate` to `endDate`); validates no self-borrowing and no overlapping reservations. |
| `GET` | `/api/v1/borrow-requests/my-requests` | Bearer | List borrow requests initiated by the current student. |
| `GET` | `/api/v1/borrow-requests/received` | Bearer | List incoming borrow requests for items owned by the current student. |
| `PUT` | `/api/v1/borrow-requests/{id}/approve` | Bearer (Lender) | Approve request, reserve item, and reject conflicting overlapping requests. |
| `PUT` | `/api/v1/borrow-requests/{id}/reject` | Bearer (Lender) | Reject a pending borrow request with response note. |
| `PUT` | `/api/v1/borrow-requests/{id}/cancel` | Bearer (Borrower) | Cancel a pending or approved borrow request before handover. |
| `PUT` | `/api/v1/borrow-requests/{id}/handover` | Bearer | Confirm physical item handover and activate transaction (`ACTIVE`). |
| `PUT` | `/api/v1/borrow-requests/{id}/return` | Bearer | Initiate or confirm item return, record on-time status, release item to `AVAILABLE`, and recalculate trust scores. |
| `POST` | `/api/v1/exchanges` | Bearer | Propose an item-for-item exchange linking `requestedItemId` and `offeredItemId`. |
| `GET` | `/api/v1/exchanges` | Bearer | List all sent and received exchange proposals for the current student. |
| `GET` | `/api/v1/exchanges/{id}` | Bearer | Retrieve full details of an exchange offer. |
| `PUT` | `/api/v1/exchanges/{id}/accept` | Bearer (Receiver) | Accept exchange proposal and reserve both items. |
| `PUT` | `/api/v1/exchanges/{id}/reject` | Bearer (Receiver) | Reject exchange proposal. |
| `PUT` | `/api/v1/exchanges/{id}/complete` | Bearer | Record participant handover confirmation; completes exchange once both parties confirm. |
| `POST` | `/api/v1/rentals` | Bearer | Create a rental booking; backend calculates `rentalDays` and `totalRentalAmount = rentalDays × dailyRate`. |
| `GET` | `/api/v1/rentals` | Bearer | List rentals where the current user is renter or owner. |
| `GET` | `/api/v1/rentals/{id}` | Bearer | Retrieve rental agreement details. |
| `PUT` | `/api/v1/rentals/{id}/approve` | Bearer (Owner) | Approve rental booking and reserve listing. |
| `PUT` | `/api/v1/rentals/{id}/complete` | Bearer (Owner) | Confirm rental return, refund security deposit, and complete transaction. |
| `POST` | `/api/v1/donations/request` | Bearer | Submit a motivation request for a `DONATE` listing. |
| `GET` | `/api/v1/donations/my-requests` | Bearer | List donation requests submitted or received by the current user. |
| `PUT` | `/api/v1/donations/{id}/approve` | Bearer (Donor) | Select recipient, auto-reject competing requests, and complete donation transfer. |
| `PUT` | `/api/v1/donations/{id}/reject` | Bearer (Donor) | Decline a donation request. |
| `GET` | `/api/v1/transactions` | Bearer | List unified transaction ledger entries for the authenticated user. |
| `GET` | `/api/v1/transactions/{id}` | Bearer | Retrieve specific transaction ledger record. |

---

## 5. Community, Trust, Moderation & Admin Endpoints
| Method | Endpoint | Auth | Description |
| :---: | :--- | :---: | :--- |
| `POST` | `/api/v1/reviews` | Bearer | Submit a 1–5 star peer review for a `COMPLETED` transaction (one review per participant per transaction). |
| `GET` | `/api/v1/reviews/{id}` | Bearer | Retrieve a specific review by ID. |
| `GET` | `/api/v1/notifications` | Bearer | Retrieve unread notification count and notification list. |
| `PUT` | `/api/v1/notifications/{id}/read` | Bearer | Mark a single notification as read. |
| `PUT` | `/api/v1/notifications/read-all` | Bearer | Mark all user notifications as read. |
| `POST` | `/api/v1/disputes` | Bearer | File a dispute or report an inappropriate listing/user. |
| `GET` | `/api/v1/disputes/my-disputes` | Bearer | List disputes filed by or involving the current student. |
| `GET` | `/api/v1/admin/disputes` | Admin/Mod | List all platform disputes for moderation review. |
| `PUT` | `/api/v1/admin/disputes/{id}/resolve` | Admin/Mod | Resolve or dismiss a dispute, record resolution notes, and optionally apply a trust score penalty. |
| `GET` | `/api/v1/admin/dashboard` | Admin/Mod | Retrieve all 11 live platform KPIs. |
| `GET` | `/api/v1/admin/analytics` | Admin/Mod | Retrieve all datasets for the 10 platform analytics charts (`?range=7d\|30d\|3m\|6m\|custom`). |
| `GET` | `/api/v1/admin/users` | Admin/Mod | Retrieve complete user directory with verification and trust metrics. |
| `PUT` | `/api/v1/admin/users/{id}/status` | Admin | Activate, verify, or suspend a user account and log audit event. |
| `GET` | `/api/v1/admin/items` | Admin/Mod | Retrieve all campus listings for administrative inspection. |
| `GET` | `/api/v1/admin/transactions` | Admin/Mod | Retrieve all campus transactions across all four workflows. |
| `GET` | `/api/v1/admin/audit-logs` | Admin/Mod | Retrieve immutable security and moderation audit logs. |
