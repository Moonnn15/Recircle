# ReCircle — A Trust-Based Campus Community Resource Sharing and Exchange Management System

> **Tagline:** *Share More. Waste Less. Build Trust.*  
> **Academic Context:** Bachelor of Technology in Computer Science and Engineering — Full Stack Development Project  
> **Institution:** SRM Institute of Science and Technology, Kattankulathur – 603 203

---

## 1. Executive Overview

**ReCircle** is a full-stack, trust-driven campus community resource-sharing and exchange management platform designed for university ecosystems. Students frequently require textbooks, scientific calculators, laboratory equipment, microcontrollers, bicycles, electronics, sports equipment, and hostel essentials, while peers on the same campus own underutilized items. ReCircle eliminates redundant purchases and promotes circular sustainability (**UN SDG 11 & SDG 12**) by enabling verified students to **Borrow, Lend, Rent, Exchange, and Donate** resources with full accountability.

### Core Platform Capabilities & Enforced Business Rules
- **Campus-Verified Authentication & RBAC (`US01`–`US05`):** BCrypt password hashing (`strength=12`), HMAC-SHA256 JWT access and refresh tokens, campus email domain enforcement (`@srmist.edu.in`), single-use expiring OTP email verification, password recovery, and role-based access control across **STUDENT**, **MODERATOR**, and **ADMIN** roles.
- **Date-Based Resource Availability (`3.A`):**
  - Items remain `AVAILABLE` for non-overlapping date ranges when future borrow or rental reservations are approved.
  - Database-level row locking (`SELECT ... FOR UPDATE` via `@Lock(LockModeType.PESSIMISTIC_WRITE)`) and `@Version` optimistic locking protect against concurrent double bookings.
  - Overlapping `PENDING` requests for the same date window are automatically rejected upon approval of a reservation.
  - `GET /api/v1/items/{id}/availability?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` computes date-range availability and returns all reserved intervals.
- **Payment Integrity (`3.B`):**
  - Approving a rental request or confirming handover **never** fabricates `paymentStatus = PAID`.
  - Without a live payment gateway, rental payment status remains `PENDING` (or `NOT_INTEGRATED`) unless the requester explicitly opts into simulated demo payment (`simulateImmediatePayment = true`).
- **Four-Pillar Dynamic Trust Score Engine & Resolved Dispute Penalty (`3.C`):**
  $$\text{TrustScore}(u) = \text{clamp}_{[0, 100]}\Big(\text{round}_2\big(0.15 \cdot V(u) + 0.35 \cdot T(u) + 0.30 \cdot R(u) + 0.20 \cdot O(u) - 15.00 \cdot D_{\text{resolved}}(u)\big)\Big)$$
  - Neutral `50.00` baseline for new users on review and on-time return pillars until eligible history exists.
  - Dispute penalties (`-15.00`) are applied strictly when an authorized **MODERATOR** or **ADMIN** resolves a dispute with `penalizeReportedUser = true`.
  - Every recalculation writes an immutable audit record to `trust_score_history`.
- **Workflow State-Transition Integrity (`3.D`):**
  - Explicit state machines across **Borrowing**, **Exchanges**, **Rentals**, and **Donations** with actor/ownership verification and atomic `transactions` ledger synchronization.

---

## 2. Verified Toolchain & Dependency Versions

| Layer | Component | Verified Version | Verification Notes |
| :--- | :--- | :--- | :--- |
| **Java Runtime & Compiler** | OpenJDK 21 LTS | **`21.0.6`** (`LTS`) | Configured automatically in `backend/mvnw.cmd` (`C:\Program Files\Android\Android Studio\jbr`, fallback `21.0.5`) |
| **Backend Framework** | Spring Boot | **`3.2.5`** | Spring Framework `6.1.6`, Hibernate ORM `6.4.4.Final`, Tomcat `10.1.20` |
| **Build Tool** | Apache Maven Wrapper | **`3.8.4`** | Maven Compiler Plugin `3.11.0` (`<release>21</release>`) |
| **Relational Database** | MySQL Community Server | **`9.5.0`** (`InnoDB`, `utf8mb4`) | `21` tables, `37` foreign keys, `20` enforced `CHECK` constraints, `10` unique constraints verified on MySQL `9.5.0` |
| **Frontend Framework** | React + TypeScript | **`19.2.3` + `5.8.3`** | Built with **Vite `8.1.3`** and **Tailwind CSS `4.2.0`** |

---

## 3. Repository Directory Structure

```text
recircle/
├── frontend/                   # React 19.2.3 + TypeScript 5.8.3 + Vite 8.1.3 + Tailwind CSS 4.2.0 SPA
│   ├── src/
│   │   ├── api/                # Axios HTTP client with JWT interceptor
│   │   ├── components/         # AppLayout sidebar/header shell & ItemCard components
│   │   ├── context/            # AuthContext session & notification management
│   │   ├── pages/              # Auth, Dashboard, Items, Sharing, Profile, Admin & Moderator views
│   │   └── types/              # TypeScript interfaces matching backend DTOs
├── backend/                    # OpenJDK 21.0.6 + Spring Boot 3.2.5 REST API (/api/v1/*)
│   ├── src/main/java/com/recircle/
│   │   ├── config/             # CORS, WebConfig & DataInitializer (seeds 10 categories, 6 users, 8 items)
│   │   ├── controller/         # REST Controllers (Health, Auth/Users, Categories/Items, Workflows, Admin)
│   │   ├── dto/                # Request/Response Java records
│   │   ├── entity/             # 21 JPA Entities mapped to normalized MySQL 9.5 tables
│   │   ├── enums/              # 12 domain state & role enumerations
│   │   ├── exception/          # GlobalExceptionHandler & ApiException
│   │   ├── mapper/             # Entity-to-DTO mapper
│   │   ├── repository/         # 21 Spring Data JPA Repositories with PESSIMISTIC_WRITE locks
│   │   ├── scheduler/          # ReturnReminderScheduler (overdue & due-soon cron job)
│   │   ├── security/           # BCrypt PasswordHasher, JwtService, JwtAuthenticationFilter
│   │   ├── service/            # Auth, Item, SharingWorkflow, TrustScore, ReviewDispute & Admin services
│   │   └── validation/         # Strict server-side InputValidator
│   └── src/test/java/          # JUnit 5 + MockMvc integration test suite (30 test methods, TC01–TC42)
├── database/                   # MySQL 9.5.0 Schema, Seed Data & Database Setup Guide
│   ├── schema.sql              # 21 normalized InnoDB utf8mb4 tables, 37 FKs, 20 CHECK constraints
│   ├── seed.sql                # Idempotent seed data (10 categories, 6 users, 8 items, sample workflows)
│   ├── README.md               # MySQL 9.5 verification & setup instructions
│   └── DATABASE_DESIGN.md      # Mermaid ER diagram, 1NF–BCNF analysis & indexing strategy
├── documentation/              # Complete Technical, Architectural, API, Testing & Sprint Documentation
│   ├── architecture/
│   │   ├── SYSTEM_ARCHITECTURE.md
│   │   └── DATABASE_ER_DIAGRAM.md
│   ├── api/
│   │   └── API_DOCUMENTATION.md
│   ├── testing/
│   │   ├── TESTING_REPORT.md
│   │   ├── SPRINT_1_AUDIT.md
│   │   ├── SPRINT_2_AUDIT.md
│   │   └── SPRINT_3_AUDIT.md
│   └── sprint-reports/
│       ├── SPRINT_1_REPORT.md
│       ├── SPRINT_2_REPORT.md
│       └── SPRINT_3_REPORT.md
├── report/                     # SRM B.Tech CSE Academic Project Report (.docx & .md + figures)
│   ├── ReCircle_Final_Report.docx
│   ├── ReCircle_SRM_Project_Report.docx
│   ├── ReCircle_SRM_Project_Report.md
│   ├── generate_srm_report.py
│   └── figures/
├── screenshots/                # 26 high-resolution rendered UI screenshots covering all 3 Sprints
├── .env.example                # Documented environment variable template
├── .gitignore                  # Clean Git exclusions
└── README.md                   # Project setup, toolchain verification, and execution guide
```

---

## 4. Quick Start & Verification Guide

### Step 1: Verify or Initialize MySQL 9.5 Database (`recircle_db`)
```powershell
& "C:\Program Files\MySQL\MySQL Server 9.5\bin\mysql.exe" -u root -p < recircle\database\schema.sql
& "C:\Program Files\MySQL\MySQL Server 9.5\bin\mysql.exe" -u root -p recircle_db < recircle\database\seed.sql
```

### Step 2: Start the Spring Boot Backend (`http://localhost:8080`)
```powershell
cd recircle\backend
.\mvnw.cmd spring-boot:run
```
- Health-check endpoint: `GET http://localhost:8080/api/v1/health`
- To run against an external MySQL 9.5 server (`recircle_db`), pass `-Dspring-boot.run.profiles=mysql` and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` (see `.env.example`).

### Step 3: Start the React + TypeScript Frontend (`http://localhost:5173`)
```powershell
cd recircle\frontend
npm run dev
```
- Production build verification:
```powershell
node recircle\frontend\node_modules\vite\bin\vite.js build recircle\frontend
```

### Step 4: Demo Campus Credentials (`seed.sql` & `DataInitializer`)
| Role | Full Name | Campus Email | Password | Reg. Number |
| :--- | :--- | :--- | :--- | :--- |
| **Student** | Aarav Krishnan | `aarav.krishnan@srmist.edu.in` | `ReCircle@2026` | `RA2211003010101` |
| **Student** | Diya Nair | `diya.nair@srmist.edu.in` | `ReCircle@2026` | `RA2211003010142` |
| **Student** | Rohan Verma | `rohan.verma@srmist.edu.in` | `ReCircle@2026` | `RA2211003010088` |
| **Student** | Meera Sundaram | `meera.sundaram@srmist.edu.in` | `ReCircle@2026` | `RA2311003010215` |
| **Moderator** | Vikram Subramanian | `moderator@srmist.edu.in` | `ReCircle@2026` | `RA2111003010019` |
| **Administrator** | Dr. Senthil Kumar | `admin@srmist.edu.in` | `ReCircle@2026` | `SRM-ADMIN-0001` |
