# ReCircle — A Trust-Based Campus Community Resource Sharing and Exchange Management System

> **Tagline:** *Share More. Waste Less. Build Trust.*  
> **Academic Context:** Bachelor of Technology in Computer Science and Engineering — Full Stack Development (`21CSP401L / 21CSP402L`)  
> **Institution:** SRM Institute of Science and Technology, Kattankulathur – 603 203

---

## 1. Executive Overview

**ReCircle** is a full-stack, trust-driven campus community resource-sharing and exchange management platform designed for university ecosystems. Students frequently require textbooks, scientific calculators, laboratory equipment, microcontrollers, bicycles, electronics, sports equipment, and hostel essentials, while peers on the same campus own underutilized items. ReCircle eliminates redundant purchases and promotes circular sustainability (**UN SDG 11 & SDG 12**) by enabling verified students to **Borrow, Lend, Rent, Exchange, and Donate** resources with full accountability.

### Core Platform Capabilities
- **Campus-Verified Authentication & RBAC:** BCrypt password hashing (`strength=12`), HMAC-SHA256 JWT access and refresh tokens, campus email domain enforcement (`@srmist.edu.in`), OTP email verification, password recovery, and role-based access control across **STUDENT**, **MODERATOR**, and **ADMIN** roles.
- **Multi-Mode Resource Sharing Workflows:**
  1. **Borrowing & Lending:** Date-range reservation validation, conflict-free overlap detection, owner approval, physical handover confirmation, scheduled return reminders, on-time return tracking, and overdue detection.
  2. **Peer-to-Peer Item Exchanges:** Barter proposals linking a proposer's eligible listing with a receiver's listing, dual handover verification, and simultaneous completion.
  3. **Structured Micro-Rentals:** Backend-calculated daily rental pricing (`rentalDays × dailyRate`) plus refundable security deposits, payment status tracking, and return verification.
  4. **Campus Donations:** Zero-cost resource gifting with motivation statements, donor recipient selection, and single-recipient fulfillment protection.
- **Weighted Dynamic Trust Score Engine (`0`–`100` Scale):**
  $$\text{Trust Score} = 0.15 \times V_{\text{account}} + 0.35 \times C_{\text{transactions}} + 0.30 \times R_{\text{reviews}} + 0.20 \times O_{\text{ontime}}$$
  Automatically recalculates and records an immutable audit entry in `trust_score_history` upon email verification, transaction completion, peer rating submission, or late/disputed return.
- **Administrative & Moderation Governance Suite:** 11 real-time platform KPIs, 10 interactive Recharts analytics visualizations with date-range filtering, user verification/suspension controls, category management, dispute arbitration, and an immutable security audit log.

---

## 2. Repository Directory Structure

```text
recircle/
├── frontend/                   # React 19 + TypeScript + Vite + Tailwind CSS + Recharts SPA (32 Views)
│   ├── src/
│   │   ├── api/                # Axios HTTP client with JWT interceptor
│   │   ├── components/         # AppLayout sidebar/header shell & ItemCard components
│   │   ├── context/            # AuthContext session & notification management
│   │   ├── pages/              # Auth, Dashboard, Items, Sharing, Profile, Admin & Moderator views
│   │   └── types/              # TypeScript interfaces matching backend DTOs
├── backend/                    # Java 21 + Spring Boot 3.2 REST API (/api/v1/*)
│   ├── src/main/java/com/recircle/
│   │   ├── config/             # CORS, WebConfig & DataInitializer (seeds 10 categories, 6 users, 8 items)
│   │   ├── controller/         # REST Controllers (Auth, Items, Workflows, Community & Admin)
│   │   ├── dto/                # Request/Response Java records
│   │   ├── entity/             # 21 JPA Entities mapped to normalized MySQL tables
│   │   ├── enums/              # 12 domain state & role enumerations
│   │   ├── exception/          # GlobalExceptionHandler & ApiException
│   │   ├── mapper/             # Entity-to-DTO mapper
│   │   ├── repository/         # 21 Spring Data JPA Repositories
│   │   ├── scheduler/          # ReturnReminderScheduler (overdue & due-soon cron job)
│   │   ├── security/           # BCrypt PasswordHasher, JwtService, JwtAuthenticationFilter
│   │   ├── service/            # Business logic & TrustScoreService
│   │   └── validation/         # Strict server-side InputValidator
│   └── src/test/java/          # JUnit 5 + MockMvc integration test suite (TC01–TC30)
├── database/                   # MySQL 8/9 Schema, Seed Data & ER/Normalization Design
│   ├── schema.sql              # 21 normalized tables with foreign keys, checks & composite indexes
│   ├── seed.sql                # Initial SRM campus categories, demo users, items & trust scores
│   └── DATABASE_DESIGN.md      # Mermaid ER diagram, 1NF–BCNF analysis & indexing strategy
├── documentation/              # Complete Technical & Architectural Documentation
│   ├── PROJECT_OVERVIEW.md
│   ├── INSTALLATION_AND_SETUP.md
│   ├── SYSTEM_ARCHITECTURE.md
│   ├── API_DOCUMENTATION.md
│   └── TESTING_REPORT.md
├── report/                     # SRM B.Tech CSE Academic Project Report (.docx & .md + figures)
│   ├── ReCircle_SRM_Project_Report.docx
│   ├── ReCircle_SRM_Project_Report.md
│   └── figures/
├── screenshots/                # High-resolution rendered UI screenshots of all core modules
├── .env.example                # Documented environment variable template
└── .gitignore                  # Clean Git exclusions
```

---

## 3. Quick Start Guide

### Prerequisites
- **Java JDK:** 21 or newer (tested with Eclipse Adoptium OpenJDK 21 / 25)
- **Maven:** 3.8+ (or use `./backend/mvnw.cmd`)
- **Node.js:** 20+ / 22+ with npm
- **Database:** MySQL 8.0+ / 9.0+ (for production/MySQL profile) or built-in H2 MySQL-compatibility mode (zero-config local startup)

### Step 1: Start the Spring Boot Backend (`http://localhost:8080`)
```powershell
cd recircle/backend
./mvnw.cmd spring-boot:run
```
> By default, the backend starts on port `8080`, initializes the schema, and automatically seeds **10 campus categories**, **6 verified SRM accounts**, **8 campus resource listings**, sample transactions, reviews, and disputes. To run against an external MySQL server (`recircle_db`), activate `-Dspring-boot.run.profiles=mysql`.

### Step 2: Start the React + TypeScript Frontend (`http://localhost:5173`)
```powershell
cd recircle/frontend
npm install
npm run dev
```

### Step 3: Demo Campus Credentials
| Role | Full Name | Campus Email | Password | Reg. Number |
| :--- | :--- | :--- | :--- | :--- |
| **Student** | Aarav Krishnan | `aarav.krishnan@srmist.edu.in` | `Student@123` | `RA2211003010101` |
| **Student** | Diya Nair | `diya.nair@srmist.edu.in` | `Student@123` | `RA2211003010142` |
| **Student** | Rohan Verma | `rohan.verma@srmist.edu.in` | `Student@123` | `RA2211042010088` |
| **Moderator** |Vikram Subramanian | `moderator@srmist.edu.in` | `Moderator@123` | `RA2111003010019` |
| **Administrator** | Dr. Senthil Kumar | `admin@srmist.edu.in` | `Admin@123` | `SRM-ADMIN-001` |

---

## 4. Verification & Automated Testing (`TC01`–`TC30`)
All **30 functional, workflow, security, and trust-score test cases** are automated in `ReCircleBackendIntegrationTest.java` and verified with `17/17` JUnit 5 test methods passing (`0` errors). See [`documentation/TESTING_REPORT.md`](documentation/TESTING_REPORT.md) for the complete execution log.
