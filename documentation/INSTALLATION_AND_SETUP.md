# ReCircle — Installation, Environment Configuration & Setup Guide

## 1. System Requirements
| Component | Version / Specification |
| :--- | :--- |
| **Java Development Kit (JDK)** | Java 21 or newer (Eclipse Adoptium OpenJDK 21 / 25 supported) |
| **Build Tool** | Apache Maven 3.8+ (included via `./backend/mvnw.cmd`) |
| **Frontend Runtime** | Node.js 20.x / 22.x LTS and npm 10+ |
| **Relational Database** | MySQL Server 8.0+ / 9.0+ (`recircle_db`) or H2 In-Memory/File MySQL Mode |
| **Operating System** | Windows 10/11, macOS, or Linux |

---

## 2. Environment Variable Configuration (`.env`)
Copy `.env.example` in the project root to `.env` or set the corresponding system environment variables before starting the services:

```ini
# Server & Database Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=default
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=recircle_db
MYSQL_USERNAME=recircle_user
MYSQL_PASSWORD=ChangeMeInProduction!

# JWT & Security Configuration
RECIRCLE_JWT_SECRET=ReCircleCampusTrustSharingSecretKey2026SRMInstituteOfScienceAndTech!
RECIRCLE_JWT_EXPIRATION_MS=86400000
RECIRCLE_REFRESH_EXPIRATION_MS=604800000

# Campus Domain Policy & Upload Limits
RECIRCLE_ALLOWED_DOMAINS=srmist.edu.in,campus.edu,edu.in
RECIRCLE_UPLOAD_DIR=./uploads
RECIRCLE_MAX_UPLOAD_BYTES=5242880

# Frontend API Target
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## 3. Database Setup (MySQL Production Profile)
If running with a standalone MySQL 8/9 server:
1. Log into MySQL CLI or MySQL Workbench:
   ```sql
   CREATE DATABASE IF NOT EXISTS recircle_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Execute the normalized 21-table schema and seed scripts:
   ```bash
   mysql -u root -p recircle_db < database/schema.sql
   mysql -u root -p recircle_db < database/seed.sql
   ```
3. Alternatively, run the backend with the default zero-configuration profile, which automatically initializes all 21 tables in MySQL-compatibility mode and runs `DataInitializer.java` to seed all categories, users, listings, and transactions.

---

## 4. Running the Backend Service (`recircle/backend`)
```powershell
cd recircle/backend
# Compile and start the Spring Boot server on http://localhost:8080
.\mvnw.cmd spring-boot:run
```
- **API Base Path:** `http://localhost:8080/api/v1`
- **Local Email / OTP Testing Mode:** For local development without external SMTP credentials, registration and password-recovery endpoints return `devOtpPreview` / `devResetToken` in the JSON payload and log the verification code to the backend console.

---

## 5. Running the Frontend Application (`recircle/frontend`)
```powershell
cd recircle/frontend
# Install dependencies (if not already present)
npm install

# Start Vite development server on http://localhost:5173
npm run dev

# Build production bundle into recircle/frontend/dist
npm run build
```
