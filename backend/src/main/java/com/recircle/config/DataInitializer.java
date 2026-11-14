package com.recircle.config;

import com.recircle.entity.*;
import com.recircle.enums.*;
import com.recircle.repository.*;
import com.recircle.security.PasswordHasher;
import com.recircle.service.NotificationService;
import com.recircle.service.TrustScoreService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the 10 campus categories and realistic SRM Kattankulathur demo data when database is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final ExchangeOfferRepository exchangeOfferRepository;
    private final RentalRepository rentalRepository;
    private final DonationRequestRepository donationRequestRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final ReviewRepository reviewRepository;
    private final DisputeRepository disputeRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordHasher passwordHasher;
    private final TrustScoreService trustScoreService;
    private final NotificationService notificationService;

    public DataInitializer(
        CategoryRepository categoryRepository,
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        ItemRepository itemRepository,
        ItemImageRepository itemImageRepository,
        BorrowRequestRepository borrowRequestRepository,
        ExchangeOfferRepository exchangeOfferRepository,
        RentalRepository rentalRepository,
        DonationRequestRepository donationRequestRepository,
        TransactionRecordRepository transactionRecordRepository,
        ReviewRepository reviewRepository,
        DisputeRepository disputeRepository,
        AuditLogRepository auditLogRepository,
        PasswordHasher passwordHasher,
        TrustScoreService trustScoreService,
        NotificationService notificationService
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.borrowRequestRepository = borrowRequestRepository;
        this.exchangeOfferRepository = exchangeOfferRepository;
        this.rentalRepository = rentalRepository;
        this.donationRequestRepository = donationRequestRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.reviewRepository = reviewRepository;
        this.disputeRepository = disputeRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordHasher = passwordHasher;
        this.trustScoreService = trustScoreService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        // 1. Seed all 10 required categories
        Category c1 = saveCategory("Books and Academic Materials", "books-academic", "Engineering textbooks, competitive exam guides, reference manuals, and lecture notes.", "BookOpen");
        Category c2 = saveCategory("Electronics and Gadgets", "electronics-gadgets", "Scientific calculators, Arduino/Raspberry Pi kits, chargers, adapters, and peripherals.", "Cpu");
        Category c3 = saveCategory("Laboratory Equipment", "laboratory-equipment", "Lab coats, drafters, breadboards, multimeters, safety goggles, and dissection kits.", "FlaskConical");
        Category c4 = saveCategory("Stationery", "stationery", "Engineering drawing instruments, geometry sets, whiteboard markers, and organizers.", "PenTool");
        Category c5 = saveCategory("Sports Equipment", "sports-equipment", "Badminton rackets, footballs, cricket gear, yoga mats, and fitness accessories.", "Trophy");
        Category c6 = saveCategory("Bicycles and Transportation", "bicycles-transport", "Campus bicycles, helmets, locks, and kick scooters for commuting between hostels and Tech Park.", "Bike");
        Category c7 = saveCategory("Hostel Essentials", "hostel-essentials", "Study lamps, extension cords, laundry baskets, electric kettles, and storage organizers.", "Lamp");
        Category c8 = saveCategory("Clothing and Accessories", "clothing-accessories", "Blazers for placement interviews, workshop aprons, convocation attire, and umbrellas.", "Shirt");
        Category c9 = saveCategory("Musical Instruments", "musical-instruments", "Acoustic guitars, ukuleles, keyboards, cajons, and music stands for campus clubs.", "Music");
        Category c10 = saveCategory("Other Resources", "other-resources", "Event props, camera tripods, board games, and miscellaneous student utilities.", "Layers");

        // 2. Seed Demo Users (Password for all demo accounts: ReCircle@2026)
        String demoHash = passwordHasher.encode("ReCircle@2026");

        User u1 = saveUser("Aarav Krishnan (Demo Student)", "aarav.krishnan@srmist.edu.in", demoHash, "RA2211003010101", UserRole.STUDENT, true,
            "Computer Science and Engineering", 4, "+91 98401 11223", "Nelson Mandela Hostel - Block B",
            "Final year B.Tech CSE student passionate about circular campus sustainability and embedded systems.", "Tech Park Ground Floor Lobby");

        User u2 = saveUser("Diya Subramanian (Demo Student)", "diya.s@srmist.edu.in", demoHash, "RA2211003010142", UserRole.STUDENT, true,
            "Electronics and Communication Engineering", 3, "+91 98402 22334", "Meenakshi Hostel - Wing A",
            "ECE 3rd year student. Happy to lend VLSI textbooks, calculators, and FPGA development boards.", "University Building Central Library Entrance");

        User u3 = saveUser("Rohan Verma (Demo Student)", "rohan.verma@srmist.edu.in", demoHash, "RA2311026010088", UserRole.STUDENT, true,
            "Mechanical Engineering", 2, "+91 98403 33445", "Paari Hostel - Room 304",
            "Sophomore Mech student. Sharing engineering drawing tools, lab gear, and campus bicycle.", "Java Green Food Court");

        User u4 = saveUser("Meera Nair (Demo Student)", "meera.nair@srmist.edu.in", demoHash, "RA2411003010215", UserRole.STUDENT, false,
            "Biotechnology", 1, "+91 98404 44556", "Kalpana Chawla Hostel",
            "First year Biotech student exploring campus community resource exchange.", "Bioengineering Block Foyer");

        User mod = saveUser("Karthik Sundaram (Campus Moderator)", "moderator@srmist.edu.in", demoHash, "MOD2026SRM01", UserRole.MODERATOR, true,
            "Student Affairs & Community Moderation", 4, "+91 98405 55667", "Main Campus Admin Annex",
            "Campus Community Moderator ensuring safe, fair, and verified student exchanges.", "Student Activity Centre (SAC)");

        User admin = saveUser("Dr. S. Venkatesh (Platform Admin)", "admin@srmist.edu.in", demoHash, "ADM2026SRM01", UserRole.ADMIN, true,
            "Department of Computing Technologies", 4, "+91 98406 66778", "Tech Park 7th Floor Faculty Wing",
            "ReCircle System Administrator overseeing campus sustainability initiatives.", "Tech Park 7th Floor");

        // 3. Seed Sample Campus Items
        Item i1 = saveItem(u1, c1, "Cormen Introduction to Algorithms (CLRS 4th Edition)",
            "Original hardcover CLRS 4th Edition in pristine condition. Ideal for Design & Analysis of Algorithms (21CSC204J) exam preparation and placement coding prep.",
            "Computer Science Textbooks", ItemCondition.LIKE_NEW, SharingType.BORROW, "Tech Park Ground Floor Lobby",
            BigDecimal.ZERO, BigDecimal.ZERO, 14, null, "Please do not highlight or write in margins with pen.",
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80");

        Item i2 = saveItem(u2, c2, "Casio fx-991CW ClassWiz Scientific Calculator",
            "University exam approved scientific calculator with high-resolution display, matrix/vector solver, and numerical integration.",
            "Calculators", ItemCondition.LIKE_NEW, SharingType.BORROW, "Central Library Entrance",
            BigDecimal.ZERO, BigDecimal.ZERO, 7, null, "Return immediately after semester exam slot finishes.",
            "https://images.unsplash.com/photo-1587145820266-a5951ee6f620?auto=format&fit=crop&w=800&q=80");

        Item i3 = saveItem(u3, c6, "Firefox Bad Attitude 21-Speed Campus Commuter Bicycle",
            "Well-maintained 21-speed geared bicycle with combination U-lock and LED night lamp. Perfect for commuting between hostels, CRC, and Tech Park.",
            "Geared Bicycles", ItemCondition.GOOD, SharingType.RENT, "Java Green Cycle Stand",
            new BigDecimal("500.00"), new BigDecimal("50.00"), 10, null, "Always lock at designated security stands. Helmet included.",
            "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=800&q=80");

        Item i4 = saveItem(u2, c3, "Arduino Mega 2560 + IoT Sensor & Breadboard Starter Kit",
            "Complete embedded systems kit containing Arduino Mega, ESP8266 Wi-Fi module, ultrasonic/DHT22 sensors, OLED display, jumper wires, and multimeter.",
            "Microcontroller Kits", ItemCondition.GOOD, SharingType.RENT, "CRC Building 2nd Floor",
            new BigDecimal("300.00"), new BigDecimal("40.00"), 14, null, "Check component checklist at handover and return.",
            "https://images.unsplash.com/photo-1553406830-ef2513450d76?auto=format&fit=crop&w=800&q=80");

        Item i5 = saveItem(u1, c2, "Logitech M331 Silent Plus Wireless Mouse & USB-C Hub",
            "Ergonomic silent wireless mouse with 7-in-1 USB-C HDMI/Ethernet adapter for hackathon presentations.",
            "Computer Peripherals", ItemCondition.LIKE_NEW, SharingType.EXCHANGE, "Tech Park 4th Floor",
            BigDecimal.ZERO, BigDecimal.ZERO, 30, "Looking to swap for a Raspberry Pi 4 case, laptop stand, or GATE CSE previous year workbook.",
            "Both items should be tested in person at Tech Park lobby.",
            "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=800&q=80");

        Item i6 = saveItem(u3, c4, "Omega Mini Drafter + Engineering Graphics Instrument Tube",
            "Complete first-year Engineering Graphics kit: stainless steel mini drafter, sheet holder tube, set squares, and French curves.",
            "Drawing Instruments", ItemCondition.GOOD, SharingType.DONATE, "Paari Hostel Reception",
            BigDecimal.ZERO, BigDecimal.ZERO, 365, null, "Free donation for any 1st-year student who needs it for Engineering Graphics.",
            "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=800&q=80");

        Item i7 = saveItem(u2, c3, "White Cotton Chemistry & Bio-Lab Coat (Size M) + Safety Goggles",
            "Freshly laundered standard SRM lab coat with anti-fog polycarbonate safety goggles.",
            "Lab Safety Gear", ItemCondition.LIKE_NEW, SharingType.DONATE, "Bioengineering Block Foyer",
            BigDecimal.ZERO, BigDecimal.ZERO, 365, null, "Donating to junior students.",
            "https://images.unsplash.com/photo-1584036561566-baf8f5f1b144?auto=format&fit=crop&w=800&q=80");

        Item i8 = saveItem(u1, c9, "Yamaha F280 Acoustic Guitar with Padded Gig Bag & Capo",
            "Warm-toned dreadnought acoustic guitar with fresh DAddario phosphor bronze strings. Available for cultural fest auditions and club practice.",
            "String Instruments", ItemCondition.GOOD, SharingType.RENT, "TP Ganesan Auditorium Entrance",
            new BigDecimal("800.00"), new BigDecimal("80.00"), 7, null, "Keep inside padded gig bag when transporting across campus.",
            "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?auto=format&fit=crop&w=800&q=80");

        Item i9 = saveItem(u3, c5, "Yonex Astrox 99 Badminton Racket Pair + Feather Shuttlecocks",
            "Two strung Yonex badminton rackets with protective thermal cover for indoor stadium courts.",
            "Racket Sports", ItemCondition.LIKE_NEW, SharingType.BORROW, "SRM Indoor Sports Complex",
            BigDecimal.ZERO, BigDecimal.ZERO, 5, null, "For indoor synthetic court use only.",
            "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=800&q=80");

        Item i10 = saveItem(u2, c7, "Wipro Garnet 10W Rechargeable LED Study Lamp",
            "Touch-dimmable eye-care desk lamp with 3 color temperatures and 4-hour backup battery for late-night exam prep.",
            "Study Lamps", ItemCondition.NEW, SharingType.EXCHANGE, "Meenakshi Hostel Gate",
            BigDecimal.ZERO, BigDecimal.ZERO, 30, "Interested in exchanging for an extension power strip or USB desk fan.",
            "Includes USB-C charging cable.",
            "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80");

        // 4. Seed Sample Completed & Active Transactions, Borrowings, Rentals, Exchanges, Donations
        BorrowRequest b1 = new BorrowRequest();
        b1.setItem(i1);
        b1.setBorrower(u2);
        b1.setLender(u1);
        b1.setStartDate(LocalDate.now().minusDays(10));
        b1.setEndDate(LocalDate.now().minusDays(3));
        b1.setPurpose("Preparing for DAA mid-semester internal assessment.");
        b1.setStatus(BorrowStatus.COMPLETED);
        b1.setOwnerResponseNote("Happy to lend! Meet at Tech Park lobby.");
        b1.setHandoverConfirmedAt(Instant.now().minusSeconds(86400 * 10));
        b1.setReturnedAt(Instant.now().minusSeconds(86400 * 4));
        b1.setReturnedOnTime(true);
        b1 = borrowRequestRepository.save(b1);

        TransactionRecord tx1 = new TransactionRecord();
        tx1.setTransactionCode("RC-BOR-202601");
        tx1.setTransactionType(SharingType.BORROW);
        tx1.setReferenceId(b1.getId());
        tx1.setPrimaryItem(i1);
        tx1.setInitiatorUser(u2);
        tx1.setCounterpartyUser(u1);
        tx1.setStatus("COMPLETED");
        tx1.setPaymentStatus(PaymentStatus.NOT_REQUIRED);
        tx1.setStartDate(b1.getStartDate());
        tx1.setDueDate(b1.getEndDate());
        tx1.setCompletedAt(b1.getReturnedAt());
        tx1.setReturnedOnTime(true);
        tx1.setNotes("Returned 1 day ahead of schedule in spotless condition.");
        tx1 = transactionRecordRepository.save(tx1);

        Rental r1 = new Rental();
        r1.setItem(i3);
        r1.setRenter(u1);
        r1.setOwner(u3);
        r1.setStartDate(LocalDate.now().minusDays(6));
        r1.setEndDate(LocalDate.now().minusDays(2));
        r1.setRentalDays(4);
        r1.setDailyRate(new BigDecimal("50.00"));
        r1.setSecurityDeposit(new BigDecimal("500.00"));
        r1.setTotalRentalAmount(new BigDecimal("200.00"));
        r1.setPaymentStatus(PaymentStatus.PAID_SIMULATED);
        r1.setStatus(RentalStatus.COMPLETED);
        r1.setHandoverConfirmedAt(Instant.now().minusSeconds(86400 * 6));
        r1.setReturnedAt(Instant.now().minusSeconds(86400 * 2));
        r1.setReturnedOnTime(true);
        r1 = rentalRepository.save(r1);

        TransactionRecord tx2 = new TransactionRecord();
        tx2.setTransactionCode("RC-REN-202602");
        tx2.setTransactionType(SharingType.RENT);
        tx2.setReferenceId(r1.getId());
        tx2.setPrimaryItem(i3);
        tx2.setInitiatorUser(u1);
        tx2.setCounterpartyUser(u3);
        tx2.setStatus("COMPLETED");
        tx2.setMonetaryAmount(new BigDecimal("200.00"));
        tx2.setPaymentStatus(PaymentStatus.PAID_SIMULATED);
        tx2.setStartDate(r1.getStartDate());
        tx2.setDueDate(r1.getEndDate());
        tx2.setCompletedAt(r1.getReturnedAt());
        tx2.setReturnedOnTime(true);
        tx2.setNotes("4-day campus bicycle rental @ ₹50/day = ₹200 (Simulated Payment).");
        tx2 = transactionRecordRepository.save(tx2);

        // Seed Active Borrow Request
        BorrowRequest b2 = new BorrowRequest();
        b2.setItem(i2);
        b2.setBorrower(u1);
        b2.setLender(u2);
        b2.setStartDate(LocalDate.now());
        b2.setEndDate(LocalDate.now().plusDays(4));
        b2.setPurpose("Needed for Numerical Methods laboratory exam.");
        b2.setStatus(BorrowStatus.PENDING);
        b2 = borrowRequestRepository.save(b2);

        TransactionRecord tx3 = new TransactionRecord();
        tx3.setTransactionCode("RC-BOR-202603");
        tx3.setTransactionType(SharingType.BORROW);
        tx3.setReferenceId(b2.getId());
        tx3.setPrimaryItem(i2);
        tx3.setInitiatorUser(u1);
        tx3.setCounterpartyUser(u2);
        tx3.setStatus("PENDING");
        tx3.setStartDate(b2.getStartDate());
        tx3.setDueDate(b2.getEndDate());
        tx3.setNotes(b2.getPurpose());
        transactionRecordRepository.save(tx3);

        // Seed Exchange Offer
        ExchangeOffer eo1 = new ExchangeOffer();
        eo1.setRequestedItem(i5);
        eo1.setOfferedItem(i10);
        eo1.setProposer(u2);
        eo1.setReceiver(u1);
        eo1.setProposalMessage("Hi Aarav! Would you like to swap your Logitech wireless mouse + USB-C hub for my brand-new Wipro rechargeable LED study lamp?");
        eo1.setStatus(ExchangeStatus.PENDING);
        exchangeOfferRepository.save(eo1);

        // Seed Donation Request
        DonationRequest dr1 = new DonationRequest();
        dr1.setItem(i6);
        dr1.setRequester(u4);
        dr1.setDonor(u3);
        dr1.setMotivationMessage("Hello! I am a first-year Biotech student taking Engineering Graphics this semester and would be grateful to use this mini drafter kit.");
        dr1.setStatus(DonationStatus.PENDING);
        donationRequestRepository.save(dr1);

        // 5. Seed Reviews on Completed Transactions
        Review rev1 = new Review();
        rev1.setTransaction(tx1);
        rev1.setItem(i1);
        rev1.setReviewer(u1);
        rev1.setReviewee(u2);
        rev1.setRating(5);
        rev1.setComment("Diya took great care of my CLRS textbook and returned it right on time at Tech Park lobby!");
        reviewRepository.save(rev1);

        Review rev2 = new Review();
        rev2.setTransaction(tx1);
        rev2.setItem(i1);
        rev2.setReviewer(u2);
        rev2.setReviewee(u1);
        rev2.setRating(5);
        rev2.setComment("Aarav's book was in Like-New condition and really helped me ace my DAA assessment. Highly trusted senior!");
        reviewRepository.save(rev2);

        Review rev3 = new Review();
        rev3.setTransaction(tx2);
        rev3.setItem(i3);
        rev3.setReviewer(u3);
        rev3.setReviewee(u1);
        rev3.setRating(5);
        rev3.setComment("Aarav returned the geared bicycle clean and locked properly at Java Green. Smooth 4-day rental!");
        reviewRepository.save(rev3);

        // Recalculate trust scores based on seeded activity
        trustScoreService.recalculateTrustScore(u1.getId(), "SEED_SYNC", "Recalculated from verified campus transactions and 5-star reviews.");
        trustScoreService.recalculateTrustScore(u2.getId(), "SEED_SYNC", "Recalculated from verified campus transactions and 5-star reviews.");
        trustScoreService.recalculateTrustScore(u3.getId(), "SEED_SYNC", "Recalculated from verified campus rental completion.");

        // 6. Seed Sample Dispute & Audit Log
        Dispute disp = new Dispute();
        disp.setTransaction(tx2);
        disp.setItem(i3);
        disp.setReporter(u3);
        disp.setReportedUser(u4);
        disp.setCategory("LATE_RETURN_INQUIRY");
        disp.setTitle("Clarification on Cycle Lock Combination Handover");
        disp.setDescription("Minor query regarding spare helmet strap adjustment after campus rental return.");
        disp.setStatus(DisputeStatus.UNDER_REVIEW);
        disp.setAssignedModerator(mod);
        disp.setResolutionNotes("Moderator inspected bicycle at Java Green stand; no damage found, closing amicably.");
        disputeRepository.save(disp);

        AuditLog al = new AuditLog();
        al.setActorUser(admin);
        al.setActorRole("ADMIN");
        al.setActionType("PLATFORM_INITIALIZED");
        al.setTargetEntity("SYSTEM");
        al.setTargetId(1L);
        al.setDetails("Initialized ReCircle campus categories, trust score weights (15/35/30/20), and moderation policies.");
        auditLogRepository.save(al);

        notificationService.createNotification(
            u1,
            "EXCHANGE_PROPOSAL_RECEIVED",
            "New Exchange Proposal from Diya Subramanian",
            "Diya offered 'Wipro Garnet 10W Rechargeable LED Study Lamp' in exchange for your Logitech Mouse & USB-C Hub.",
            "/exchanges",
            eo1.getId()
        );
    }

    private Category saveCategory(String name, String slug, String desc, String icon) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setDescription(desc);
        c.setIconName(icon);
        c.setActive(true);
        return categoryRepository.save(c);
    }

    private User saveUser(
        String fullName, String email, String hash, String regNo, UserRole role, boolean verified,
        String dept, int year, String phone, String hostel, String bio, String pickup
    ) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setRegistrationNumber(regNo);
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setEmailVerified(verified);
        u.setCampusVerified(verified);
        u.setLastLoginAt(Instant.now());
        u = userRepository.save(u);

        UserProfile p = new UserProfile();
        p.setUser(u);
        p.setDepartment(dept);
        p.setYearOfStudy(year);
        p.setContactNumber(phone);
        p.setHostelOrBlock(hostel);
        p.setBio(bio);
        p.setPreferredPickupSpot(pickup);
        p.calculateCompletion();
        userProfileRepository.save(p);

        trustScoreService.initializeTrustScore(u);
        return u;
    }

    private Item saveItem(
        User owner, Category cat, String title, String desc, String subcat,
        ItemCondition cond, SharingType sharing, String location,
        BigDecimal deposit, BigDecimal rentPerDay, int maxDays,
        String exchangePref, String rules, String imageUrl
    ) {
        Item item = new Item();
        item.setOwner(owner);
        item.setCategory(cat);
        item.setTitle(title);
        item.setDescription(desc);
        item.setSubcategory(subcat);
        item.setItemCondition(cond);
        item.setSharingType(sharing);
        item.setAvailabilityStatus(ListingStatus.AVAILABLE);
        item.setCampusLocation(location);
        item.setSecurityDeposit(deposit);
        item.setRentalPricePerDay(rentPerDay);
        item.setAvailableFrom(LocalDate.now().minusDays(5));
        item.setAvailableUntil(LocalDate.now().plusMonths(6));
        item.setMaxBorrowDays(maxDays);
        item.setExchangePreferences(exchangePref);
        item.setUsageRules(rules);
        item.setPrimaryImageUrl(imageUrl);
        item.setViewCount(25);
        Item saved = itemRepository.save(item);

        ItemImage img = new ItemImage();
        img.setItem(saved);
        img.setImageUrl(imageUrl);
        img.setDisplayOrder(0);
        itemImageRepository.save(img);

        return saved;
    }
}
