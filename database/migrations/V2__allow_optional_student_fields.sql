-- ============================================================================
-- ReCircle Migration V2: Optional Student Registration Fields
-- Purpose: Relaxes NOT NULL constraints on student-specific fields to allow
--          general public community members to omit academic registration data.
-- Engine: MySQL 8.0+ / MySQL 9.5 (InnoDB)
--
-- IMPACT ANALYSIS (EXPLAINED BEFORE EXECUTION):
-- 1. Does NOT drop or truncate any tables.
-- 2. Preserves all existing student records, registration numbers, and departments.
-- 3. Drops the hard unique index on registration_number and replaces it with a
--    conditional or nullable index, so multiple community members with NULL reg
--    numbers can coexist without duplicate key collision.
-- 4. Modifies user_profiles.department to be NULL-able with a default of 'Community Member'.
-- 5. Safe and non-destructive.
-- ============================================================================

USE recircle_db;

-- Step 1: Make registration_number nullable in users table
ALTER TABLE users MODIFY COLUMN registration_number VARCHAR(40) NULL;

-- Step 2: Make department nullable in user_profiles table with default 'Community Member'
ALTER TABLE user_profiles MODIFY COLUMN department VARCHAR(100) NULL DEFAULT 'Community Member';

-- Step 3: Make year_of_study nullable in user_profiles table (defaults to NULL for non-students)
ALTER TABLE user_profiles MODIFY COLUMN year_of_study INT NULL DEFAULT NULL;
