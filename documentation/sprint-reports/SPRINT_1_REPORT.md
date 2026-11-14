# ReCircle — Sprint 1 Execution & Completion Report

## 1. Sprint 1 Goal with User Stories
**Sprint 1 Goal**: Establish the Phase 0 engineering foundation (JDK 21.0.6, Spring Boot 3.2.5, MySQL 9.5.0 21-table schema, React 19 + TypeScript + Vite frontend, and health-check connectivity) and deliver the complete **Identity, Role-Based Access Control, Resource Listing, and Campus Resource Discovery** vertical slice (`US01` through `US07`).

| Story ID | User Story | Story Points | Priority | Status |
| :---: | :--- | :---: | :---: | :---: |
| **US01** | As a college student, I want to register using my SRM campus email (`@srmist.edu.in`) and registration number so that I can join the verified campus sharing community. | 5 | High | **COMPLETED** |
| **US02** | As a registered user, I want to log in, refresh my session token, and log out securely with JWT authentication and protected routes. | 5 | High | **COMPLETED** |
| **US03** | As a student, I want to verify my campus email with a time-limited single-use 6-digit OTP so that my account receives a Verified badge and `+15.00` Trust Score points. | 3 | High | **COMPLETED** |
| **US04** | As a student, I want to view and edit my academic profile (department, year, contact number, hostel block, bio, pickup location) and view my Trust Score summary. | 3 | High | **COMPLETED** |
| **US05** | As a platform administrator/system, I want strict role-based access control (`STUDENT`, `MODERATOR`, `ADMIN`) and resource ownership checks enforced on both frontend and backend. | 5 | High | **COMPLETED** |
| **US06** | As a student, I want to create, view, edit, upload validated images for, and archive resource listings across `BORROW`, `RENT`, `EXCHANGE`, and `DONATE` modes. | 8 | High | **COMPLETED** |
| **US07** | As a student, I want to search, filter (by category, department, sharing type, condition, availability, max price), sort, and paginate available campus resources. | 5 | High | **COMPLETED** |

---

## 2. Committed vs Completed User Stories
- **Committed User Stories (Sprint 1)**: `7` (`US01`–`US07`, `34` Story Points)
- **Completed User Stories (Sprint 1)**: `7` (`US01`–`US07`, `34` Story Points — `100%` completion rate)

---

## 3. Daily Call Progress Log (Sprint 1)
| Day | Focus Area | Completed Engineering Tasks | Blockers & Resolution |
| :---: | :--- | :--- | :--- |
| **Day 1** | Phase 0 Toolchain & MySQL 9.5 Schema | Configured OpenJDK `21.0.6` (`mvnw.cmd`), finalized 21-table MySQL 9.5 `schema.sql` & `seed.sql`, verified 0 SQL errors on MySQL 9.5.0. | Resolved MySQL 9.5 Error 3823 (`CHECK` constraint on FK column) by enforcing `reviewer_id <> reviewee_id` in `ReviewDisputeService`. |
| **Day 2** | Identity & JWT Security (`US01`–`US03`) | Implemented `AuthService`, BCrypt hashing, HS256 JWT + revocable refresh tokens, single-use time-limited OTP verification, and `expose-dev-otp` config flag. | Ensured consumed or expired OTPs are strictly rejected with `INVALID_OTP`. |
| **Day 3** | Profile & RBAC (`US04`–`US05`) | Built `UserProfile` completion calculator, `/api/v1/users/me` endpoints, `AuthContext` role guards, and 401/403 route protection. | None. |
| **Day 4** | Resource Listings & Image Upload (`US06`) | Implemented `ItemService`, `ItemCategoryController`, 5 MB MIME-validated image upload, owner-only edit/archive, and date-based availability endpoint (`GET /api/v1/items/{id}/availability`). | Separated listing status (`AVAILABLE`) from date-scoped reservation status (`3.A`). |
| **Day 5** | Resource Discovery & Integration QA (`US07`) | Built `DiscoverItemsPage`, `ItemDetailsPage`, `CreateEditListingPage`, `MyListingsPage`, and executed 18/18 JUnit 5 integration test suites on JDK 21.0.6 + production `vite build`. | All 18 JUnit test suites and Vite production build succeeded. |

---

## 4. Sprint 1 Retrospective
- **What Went Well**:
  - Pinning the backend build and test execution to genuine **OpenJDK 21.0.6** eliminated all JDK 25 bytecode/Unsafe warnings.
  - Verifying `schema.sql` and `seed.sql` directly against the installed **MySQL Server 9.5.0** engine caught MySQL 9.5-specific constraint rules early.
  - Separating catalog listing status (`AVAILABLE`) from booking date ranges (`borrow_requests` / `rentals`) enabled non-overlapping multi-date bookings on the same item (`Rule 3.A`).
- **What Could Be Improved**:
  - Large frontend bundle size can be further optimized in Sprint 3 using route-level lazy loading (`React.lazy`).
- **Action Items for Sprint 2**:
  - Expand end-to-end UI workflow walkthroughs for Borrowing, Lending, Peer Exchanges, Rentals, Donations, Unified Transaction History, Peer Reviews, and Trust Score History.
