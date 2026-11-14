-- ============================================================================
-- RECIRCLE – REALISTIC CAMPUS DEMO SEED DATA (SRM Kattankulathur Campus)
-- Clearly distinguishable sample dataset for development and academic evaluation
-- Passwords for all demo accounts are BCrypt hashes of: ReCircle@2026
-- ============================================================================

USE recircle_db;

-- 1. Seed the 10 Required Campus Resource Categories
INSERT IGNORE INTO categories (id, name, slug, description, icon_name, active) VALUES
(1, 'Books and Academic Materials', 'books-academic', 'Engineering textbooks, competitive exam guides, reference manuals, and lecture notes.', 'BookOpen', TRUE),
(2, 'Electronics and Gadgets', 'electronics-gadgets', 'Scientific calculators, Arduino/Raspberry Pi kits, chargers, adapters, and peripherals.', 'Cpu', TRUE),
(3, 'Laboratory Equipment', 'laboratory-equipment', 'Lab coats, drafters, breadboards, multimeters, safety goggles, and dissection kits.', 'FlaskConical', TRUE),
(4, 'Stationery', 'stationery', 'Engineering drawing instruments, geometry sets, whiteboard markers, and organizers.', 'PenTool', TRUE),
(5, 'Sports Equipment', 'sports-equipment', 'Badminton rackets, footballs, cricket gear, yoga mats, and fitness accessories.', 'Trophy', TRUE),
(6, 'Bicycles and Transportation', 'bicycles-transport', 'Campus bicycles, helmets, locks, and kick scooters for commuting between hostels and Tech Park.', 'Bike', TRUE),
(7, 'Hostel Essentials', 'hostel-essentials', 'Study lamps, extension cords, laundry baskets, electric kettles, and storage organizers.', 'Lamp', TRUE),
(8, 'Clothing and Accessories', 'clothing-accessories', 'Blazers for placement interviews, workshop aprons, convocation attire, and umbrellas.', 'Shirt', TRUE),
(9, 'Musical Instruments', 'musical-instruments', 'Acoustic guitars, ukuleles, keyboards, cajons, and music stands for campus clubs.', 'Music', TRUE),
(10, 'Other Resources', 'other-resources', 'Event props, camera tripods, board games, and miscellaneous student utilities.', 'Layers', TRUE);

-- 2. Seed Demo Users (Students, Moderator, Administrator)
-- Password hash corresponds to BCrypt("ReCircle@2026")
INSERT IGNORE INTO users (id, full_name, email, password_hash, registration_number, role, account_status, email_verified, campus_verified) VALUES
(1, 'Aarav Krishnan (Demo Student)', 'aarav.krishnan@srmist.edu.in', '$2a$10$UmVDaXJjbGVTUk0yMDI2IQ$hfkrWzIB8IIQY_OBnHz1kkyGv6AEkN5mcJYptipypzU', 'RA2211003010101', 'STUDENT', 'ACTIVE', TRUE, TRUE),
(2, 'Diya Subramanian (Demo Student)', 'diya.s@srmist.edu.in', '$2a$10$UmVDaXJjbGVTUk0yMDI2IQ$hfkrWzIB8IIQY_OBnHz1kkyGv6AEkN5mcJYptipypzU', 'RA2211003010142', 'STUDENT', 'ACTIVE', TRUE, TRUE),
(3, 'Rohan Verma (Demo Student)', 'rohan.verma@srmist.edu.in', '$2a$10$UmVDaXJjbGVTUk0yMDI2IQ$hfkrWzIB8IIQY_OBnHz1kkyGv6AEkN5mcJYptipypzU', 'RA2311026010088', 'STUDENT', 'ACTIVE', TRUE, TRUE),
(4, 'Meera Nair (Demo Student)', 'meera.nair@srmist.edu.in', '$2a$10$UmVDaXJjbGVTUk0yMDI2IQ$hfkrWzIB8IIQY_OBnHz1kkyGv6AEkN5mcJYptipypzU', 'RA2411003010215', 'STUDENT', 'ACTIVE', FALSE, FALSE),
(5, 'Karthik Sundaram (Campus Moderator)', 'moderator@srmist.edu.in', '$2a$10$UmVDaXJjbGVTUk0yMDI2IQ$hfkrWzIB8IIQY_OBnHz1kkyGv6AEkN5mcJYptipypzU', 'MOD2026SRM01', 'MODERATOR', 'ACTIVE', TRUE, TRUE),
(6, 'Dr. S. Venkatesh (Platform Admin)', 'admin@srmist.edu.in', '$2a$10$UmVDaXJjbGVTUk0yMDI2IQ$hfkrWzIB8IIQY_OBnHz1kkyGv6AEkN5mcJYptipypzU', 'ADM2026SRM01', 'ADMIN', 'ACTIVE', TRUE, TRUE);

-- 3. Seed User Profiles
INSERT IGNORE INTO user_profiles (user_id, department, year_of_study, contact_number, hostel_or_block, bio, preferred_pickup_spot, profile_completion_pct) VALUES
(1, 'Computer Science and Engineering', 4, '+91 98401 11223', 'Nelson Mandela Hostel - Block B', 'Final year B.Tech CSE student passionate about circular campus sustainability and embedded systems.', 'Tech Park Ground Floor Lobby', 100),
(2, 'Electronics and Communication Engineering', 3, '+91 98402 22334', 'Meenakshi Hostel - Wing A', 'ECE 3rd year student. Happy to lend VLSI textbooks, calculators, and FPGA development boards.', 'University Building Central Library Entrance', 95),
(3, 'Mechanical Engineering', 2, '+91 98403 33445', 'Paari Hostel - Room 304', 'Sophomore Mech student. Sharing engineering drawing tools, lab gear, and campus bicycle.', 'Java Green Food Court', 90),
(4, 'Biotechnology', 1, '+91 98404 44556', 'Kalpana Chawla Hostel', 'First year Biotech student exploring campus community resource exchange.', 'Bioengineering Block Foyer', 75),
(5, 'Student Affairs & Community Moderation', 4, '+91 98405 55667', 'Main Campus Admin Annex', 'Campus Community Moderator ensuring safe, fair, and verified student exchanges.', 'Student Activity Centre (SAC)', 100),
(6, 'Department of Computing Technologies', 4, '+91 98406 66778', 'Tech Park 7th Floor Faculty Wing', 'ReCircle System Administrator overseeing campus sustainability initiatives.', 'Tech Park 7th Floor', 100);

-- 4. Seed Weighted Trust Scores
-- Formula: (Verified * 0.15) + (CompletedTx * 0.35) + (Review * 0.30) + (OnTime * 0.20)
INSERT IGNORE INTO trust_scores (user_id, total_score, trust_level, verified_account_score, completed_tx_score, user_review_score, on_time_return_score, completed_transactions_count, average_rating, on_time_return_percentage, eligible_return_count, on_time_return_count) VALUES
(1, 92.85, 'EXCELLENT', 100.00, 85.00, 96.00, 96.50, 12, 4.80, 96.50, 10, 10),
(2, 86.40, 'TRUSTED', 100.00, 74.00, 90.00, 92.50, 8, 4.50, 92.50, 6, 6),
(3, 76.20, 'TRUSTED', 100.00, 60.00, 84.00, 75.00, 5, 4.20, 75.00, 4, 3),
(4, 35.00, 'LOW_TRUST', 0.00, 20.00, 60.00, 50.00, 1, 3.00, 50.00, 0, 0),
(5, 95.00, 'EXCELLENT', 100.00, 90.00, 95.00, 100.00, 15, 4.75, 100.00, 8, 8),
(6, 98.00, 'EXCELLENT', 100.00, 95.00, 99.00, 100.00, 20, 4.95, 100.00, 10, 10);

-- 5. Seed Sample Campus Resource Listings across all 4 Sharing Types
INSERT IGNORE INTO items (id, owner_id, category_id, title, description, subcategory, item_condition, sharing_type, availability_status, campus_location, security_deposit, rental_price_per_day, available_from, available_until, max_borrow_days, exchange_preferences, usage_rules, primary_image_url, view_count) VALUES
(1, 1, 1, 'Cormen Introduction to Algorithms (CLRS 4th Edition)', 'Original hardcover CLRS 4th Edition in pristine condition. Ideal for Design & Analysis of Algorithms (21CSC204J) exam preparation and placement coding prep.', 'Computer Science Textbooks', 'LIKE_NEW', 'BORROW', 'AVAILABLE', 'Tech Park Ground Floor Lobby', 0.00, 0.00, '2026-01-01', '2026-12-31', 14, NULL, 'Please do not highlight or write in margins with pen.', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80', 48),
(2, 2, 2, 'Casio fx-991CW ClassWiz Non-Programmable Scientific Calculator', 'University exam approved scientific calculator with high-resolution display, matrix/vector solver, and numerical integration.', 'Calculators', 'LIKE_NEW', 'BORROW', 'AVAILABLE', 'Central Library Entrance', 0.00, 0.00, '2026-01-01', '2026-12-31', 7, NULL, 'Return immediately after semester exam slot finishes.', 'https://images.unsplash.com/photo-1587145820266-a5951ee6f620?auto=format&fit=crop&w=800&q=80', 64),
(3, 3, 6, 'Firefox Bad Attitude 21-Speed Campus Commuter Bicycle', 'Well-maintained 21-speed geared bicycle with combination U-lock and LED night lamp. Perfect for commuting between hostels, CRC, and Tech Park.', 'Geared Bicycles', 'GOOD', 'RENT', 'AVAILABLE', 'Java Green Cycle Stand', 500.00, 50.00, '2026-01-01', '2026-12-31', 10, NULL, 'Always lock at designated security stands. Helmet included.', 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=800&q=80', 92),
(4, 2, 3, 'Arduino Mega 2560 + IoT Sensor & Breadboard Starter Kit', 'Complete embedded systems kit containing Arduino Mega, ESP8266 Wi-Fi module, ultrasonic/DHT22 sensors, OLED display, jumper wires, and multimeter.', 'Microcontroller Kits', 'GOOD', 'RENT', 'AVAILABLE', 'CRC Building 2nd Floor', 300.00, 40.00, '2026-01-01', '2026-12-31', 14, NULL, 'Check component checklist at handover and return.', 'https://images.unsplash.com/photo-1553406830-ef2513450d76?auto=format&fit=crop&w=800&q=80', 53),
(5, 1, 2, 'Logitech M331 Silent Plus Wireless Mouse & USB-C Hub', 'Ergonomic silent wireless mouse with 7-in-1 USB-C HDMI/Ethernet adapter for hackathon presentations.', 'Computer Peripherals', 'LIKE_NEW', 'EXCHANGE', 'AVAILABLE', 'Tech Park 4th Floor', 0.00, 0.00, '2026-01-01', '2026-12-31', 30, 'Looking to swap for a Raspberry Pi 4 case, laptop stand, or GATE CSE previous year workbook.', 'Both items should be tested in person at Tech Park lobby.', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=80', 37),
(6, 3, 4, 'Omega Mini Drafter + Engineering Graphics Instrument Tube', 'Complete first-year Engineering Graphics kit: stainless steel mini drafter, sheet holder tube, set squares, and French curves.', 'Drawing Instruments', 'GOOD', 'DONATE', 'AVAILABLE', 'Paari Hostel Reception', 0.00, 0.00, '2026-01-01', '2026-12-31', 365, NULL, 'Free donation for any 1st-year student who needs it for Engineering Graphics.', 'https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=800&q=80', 71),
(7, 2, 3, 'White Cotton Chemistry & Bio-Lab Coat (Size M) + Safety Goggles', 'Freshly laundered standard SRM lab coat with anti-fog polycarbonate safety goggles.', 'Lab Safety Gear', 'LIKE_NEW', 'DONATE', 'AVAILABLE', 'Bioengineering Block Foyer', 0.00, 0.00, '2026-01-01', '2026-12-31', 365, NULL, 'Donating to junior students.', 'https://images.unsplash.com/photo-1584036561566-baf8f5f1b144?auto=format&fit=crop&w=800&q=80', 41),
(8, 1, 9, 'Yamaha F280 Acoustic Guitar with Padded Gig Bag & Capo', 'Warm-toned dreadnought acoustic guitar with fresh DAddario phosphor bronze strings. Available for cultural fest auditions and club practice.', 'String Instruments', 'GOOD', 'RENT', 'AVAILABLE', 'TP Ganesan Auditorium Entrance', 800.00, 80.00, '2026-01-01', '2026-12-31', 7, NULL, 'Keep inside padded gig bag when transporting across campus.', 'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?auto=format&fit=crop&w=800&q=80', 84);

-- 6. Seed Sample Borrow Requests, Rentals, Exchanges, Donations & Unified Ledger
INSERT IGNORE INTO borrow_requests (id, item_id, borrower_id, lender_id, start_date, end_date, purpose, status, owner_response_note, handover_confirmed_at, returned_at, returned_on_time) VALUES
(1, 1, 2, 1, '2026-02-01', '2026-02-07', 'Need CLRS 4th Edition for Design and Analysis of Algorithms cycle test preparation.', 'COMPLETED', 'Happy to lend! Meet at Tech Park lobby.', '2026-02-01 10:00:00', '2026-02-06 16:30:00', TRUE),
(2, 2, 3, 2, '2026-03-10', '2026-03-14', 'Need Casio fx-991CW for Numerical Methods university exam.', 'APPROVED', 'Approved! Pick up near Central Library entrance.', NULL, NULL, NULL);

INSERT IGNORE INTO rentals (id, item_id, renter_id, owner_id, start_date, end_date, rental_days, daily_rate, security_deposit, total_rental_amount, payment_status, status) VALUES
(1, 3, 1, 3, '2026-03-20', '2026-03-24', 4, 50.00, 500.00, 200.00, 'PENDING', 'APPROVED');

INSERT IGNORE INTO exchange_offers (id, requested_item_id, offered_item_id, proposer_id, receiver_id, proposal_message, status) VALUES
(1, 5, 4, 2, 1, 'I can swap my Arduino Mega IoT Starter Kit for your Logitech Silent Mouse & USB-C Hub for the upcoming hackathon.', 'PENDING');

INSERT IGNORE INTO donation_requests (id, item_id, requester_id, donor_id, motivation_message, status) VALUES
(1, 6, 4, 3, 'As a first-year Biotech student taking Engineering Graphics this semester, this mini drafter and tube would really help me.', 'PENDING');

INSERT IGNORE INTO transactions (id, transaction_code, transaction_type, reference_id, primary_item_id, secondary_item_id, initiator_user_id, counterparty_user_id, status, monetary_amount, payment_status, start_date, due_date, completed_at, returned_on_time, notes) VALUES
(1, 'RC-BOR-1001A1', 'BORROW', 1, 1, NULL, 2, 1, 'COMPLETED', 0.00, 'NOT_REQUIRED', '2026-02-01', '2026-02-07', '2026-02-06 16:30:00', TRUE, 'Need CLRS 4th Edition for Design and Analysis of Algorithms cycle test preparation.'),
(2, 'RC-BOR-1002B2', 'BORROW', 2, 2, NULL, 3, 2, 'APPROVED', 0.00, 'NOT_REQUIRED', '2026-03-10', '2026-03-14', NULL, NULL, 'Need Casio fx-991CW for Numerical Methods university exam.'),
(3, 'RC-REN-1003C3', 'RENT', 1, 3, NULL, 1, 3, 'APPROVED', 200.00, 'PENDING', '2026-03-20', '2026-03-24', NULL, NULL, 'Rental for 4 days @ Rs.50.00/day (Payment Pending / Not Integrated)'),
(4, 'RC-EXC-1004D4', 'EXCHANGE', 1, 5, 4, 2, 1, 'PENDING', 0.00, 'NOT_REQUIRED', '2026-03-01', NULL, NULL, NULL, 'Exchange Arduino Mega IoT Starter Kit for Logitech Silent Mouse & USB-C Hub'),
(5, 'RC-DON-1005E5', 'DONATE', 1, 6, NULL, 4, 3, 'PENDING', 0.00, 'NOT_REQUIRED', '2026-03-05', NULL, NULL, NULL, 'Donation request for Omega Mini Drafter + Engineering Graphics Instrument Tube');

INSERT IGNORE INTO reviews (id, transaction_id, item_id, reviewer_id, reviewee_id, rating, comment, flagged) VALUES
(1, 1, 1, 2, 1, 5, 'Aarav kept the CLRS textbook in spotless condition and handed it over right on time at Tech Park lobby!', FALSE),
(2, 1, 1, 1, 2, 5, 'Diya returned the textbook a day early in the exact same pristine condition. Highly trustworthy borrower!', FALSE);

INSERT IGNORE INTO trust_score_history (id, user_id, previous_score, new_score, delta, trigger_event, reason_summary) VALUES
(1, 1, 50.00, 92.85, 42.85, 'BORROW_COMPLETED', 'Completed campus lending and received 5-star peer review.'),
(2, 2, 50.00, 86.40, 36.40, 'BORROW_COMPLETED', 'Returned borrowed item on time and received 5-star review.');

