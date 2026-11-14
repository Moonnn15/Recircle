# ReCircle Database Layer (`recircle_db` — MySQL 9.5)

## 1. Engine & Character Set
- **Database Engine:** MySQL Server 9.5 (`InnoDB` storage engine with row-level locking and foreign-key enforcement)
- **Character Set & Collation:** `utf8mb4` / `utf8mb4_unicode_ci`

## 2. Files in `database/`
- [`schema.sql`](./schema.sql): Complete 21-table normalized DDL (`users`, `user_profiles`, `categories`, `items`, `item_images`, `borrow_requests`, `exchange_offers`, `rentals`, `donation_requests`, `transactions`, `reviews`, `trust_scores`, `trust_score_history`, `notifications`, `disputes`, `dispute_evidence`, `reports`, `audit_logs`, `refresh_tokens`, `email_verification_tokens`, `password_reset_tokens`) with foreign keys, unique constraints, scalar `CHECK` constraints, optimistic-locking `version` columns, and composite indexes.
- [`seed.sql`](./seed.sql): Initial SRM campus data seeding the 10 resource categories, verified student/moderator/administrator accounts, sample listings across `BORROW`, `RENT`, `EXCHANGE`, and `DONATE`, and initial trust score records.
- [`DATABASE_DESIGN.md`](./DATABASE_DESIGN.md): Detailed Entity-Relationship (ER) diagram, normalization proof (`1NF` through `BCNF`), and index analysis.

## 3. MySQL 9.5 Compatibility & Execution
To initialize and seed `recircle_db` on MySQL 9.5:
```powershell
& "C:\Program Files\MySQL\MySQL Server 9.5\bin\mysql.exe" -u root -p < database\schema.sql
& "C:\Program Files\MySQL\MySQL Server 9.5\bin\mysql.exe" -u root -p < database\seed.sql
```
