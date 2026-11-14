# ReCircle — Project Overview & Functional Specification

## 1. Problem Statement & Motivation
Within large university campuses such as **SRM Institute of Science and Technology**, thousands of students purchase textbooks, scientific calculators, engineering drawing instruments, lab coats, microcontroller kits, bicycles, and hostel appliances every semester. Once a course or semester concludes, these resources sit idle in hostel rooms while incoming cohorts purchase identical new items. Existing informal channels (WhatsApp groups or notice boards) lack:
1. **Identity Verification:** No guarantee that the counterparty is an active, verified campus student.
2. **Reservation & Conflict Management:** No structured calendar or state machine to prevent double-booking or track return due dates.
3. **Accountability & Reputation:** No objective metric to reward reliable lenders/borrowers or penalize late returns and item damage.
4. **Multi-Mode Support:** No unified platform supporting free borrowing, short-term rentals, peer-to-peer bartering (exchanges), and charitable donations.

## 2. Solution Architecture: ReCircle
**ReCircle** (*Share More. Waste Less. Build Trust.*) addresses these gaps through a centralized full-stack web platform tailored for campus communities:
- **Verified Campus Identity:** Enforces institutional email domains (`@srmist.edu.in`), student registration numbers (`RA2211003010xxx`), department affiliations, and OTP verification.
- **Four Integrated Resource-Sharing Workflows:**
  - **Borrowing & Lending (`BORROW`):** Free peer-to-peer lending with date validation, overlap prevention, handover confirmation, automated return reminders, and return verification.
  - **Item Exchange (`EXCHANGE`):** Direct barter workflow where Student A proposes one of their own available items in exchange for Student B's listing, requiring dual handover confirmation.
  - **Resource Rental (`RENT`):** Structured daily rentals where the backend computes `totalRentalAmount = rentalDays × dailyRate` alongside refundable security deposits.
  - **Resource Donation (`DONATE`):** Graduating seniors and students can donate books, stationery, and essentials to juniors, selecting a single recipient based on motivation statements.
- **Dynamic Trust Score Engine (`0`–`100`):**
  Every user starts with a baseline score and builds reputation across four weighted pillars:
  - **Verified Account (`15%`):** Full credit (`100`) once campus email and registration credentials are verified (`40` when unverified).
  - **Completed Transactions (`35%`):** Scales progressively by `min(100, 50 + completedCount × 10)`.
  - **Peer Reviews (`30%`):** Derived from 1–5 star ratings (`averageRating × 20`, or `75` neutral baseline prior to first review).
  - **On-Time Return Ratio (`20%`):** Percentage of borrow/rental transactions returned on or before the due date (`100%` baseline prior to first return).

## 3. Target User Roles & Permissions
| Role | Key Responsibilities & Capabilities |
| :--- | :--- |
| **STUDENT** | Register with campus email, verify OTP, manage profile, publish/edit/archive listings, search/filter marketplace, request borrows/rentals/exchanges/donations, confirm handovers/returns, rate counterparties, track trust score, and file disputes. |
| **MODERATOR** | Inspect flagged listings, investigate reported users, arbitrate transaction disputes, append resolution notes, and maintain moderation records. |
| **ADMINISTRATOR** | Full platform oversight: view 11 live KPIs and 10 Recharts analytics charts, verify or suspend student accounts, create/update campus categories, monitor all listings and transactions, resolve disputes with trust-score penalties, and audit security logs. |

## 4. Alignment with United Nations Sustainable Development Goals (SDGs)
- **SDG 11 (Sustainable Cities and Communities):** Builds a resilient, cooperative, and resource-efficient university campus community.
- **SDG 12 (Responsible Consumption and Production):** Directly curtails e-waste, paper waste, and redundant manufacturing demand by extending the lifecycle of academic and hostel goods.
