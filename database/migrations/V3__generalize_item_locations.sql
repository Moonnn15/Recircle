-- ============================================================================
-- ReCircle Migration V3: Generalize Item Locations
-- Purpose: Adds optional structured location columns (city, neighborhood, landmark,
--          pickup_instructions) to the items table while preserving campus_location
--          for complete backward compatibility.
-- Engine: MySQL 8.0+ / MySQL 9.5 (InnoDB)
--
-- IMPACT ANALYSIS:
-- 1. Does NOT drop, rename, or truncate any existing tables or columns.
-- 2. Preserves all existing item listings and legacy campus_location strings.
-- 3. Adds nullable city, neighborhood, landmark, pickup_instructions columns.
-- 4. Safe, non-destructive, and fully backward-compatible.
-- ============================================================================

USE recircle_db;

-- Step 1: Add city column to items table
ALTER TABLE items ADD COLUMN IF NOT EXISTS city VARCHAR(100) NULL AFTER campus_location;

-- Step 2: Add neighborhood / locality column to items table
ALTER TABLE items ADD COLUMN IF NOT EXISTS neighborhood VARCHAR(100) NULL AFTER city;

-- Step 3: Add landmark column to items table
ALTER TABLE items ADD COLUMN IF NOT EXISTS landmark VARCHAR(150) NULL AFTER neighborhood;

-- Step 4: Add pickup instructions column to items table
ALTER TABLE items ADD COLUMN IF NOT EXISTS pickup_instructions VARCHAR(300) NULL AFTER landmark;

-- Step 5: Add composite index for city and neighborhood discovery
ALTER TABLE items ADD INDEX IF NOT EXISTS idx_items_city_neighborhood (city, neighborhood);
