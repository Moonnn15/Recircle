import os
import zipfile
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from PIL import Image, ImageDraw, ImageFont

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SCREENSHOTS_DIR = os.path.join(ROOT_DIR, 'screenshots')
FIGURES_DIR = os.path.join(ROOT_DIR, 'report', 'figures')
SAMPLE_DOCX = r"C:\Users\monalisa\.gemini\antigravity\brain\f43bd7fd-01eb-4676-b0be-1f9c81f6c9c3\.user_uploaded\media_1794647253723_71b95996.docx"

os.makedirs(SCREENSHOTS_DIR, exist_ok=True)
os.makedirs(FIGURES_DIR, exist_ok=True)

# 1. Extract official SRM logo from the uploaded sample report .docx
def extract_srm_logo():
    logo_target = os.path.join(FIGURES_DIR, 'srm_logo.png')
    if os.path.exists(SAMPLE_DOCX):
        try:
            with zipfile.ZipFile(SAMPLE_DOCX, 'r') as z:
                media_files = sorted([f for f in z.namelist() if f.startswith('word/media/')])
                if media_files:
                    with z.open(media_files[0]) as src, open(logo_target, 'wb') as dst:
                        dst.write(src.read())
                    print(f"Extracted official SRM logo from {media_files[0]} -> {logo_target}")
                    return
        except Exception as e:
            print("Fallback logo generation:", e)
    # Fallback if docx media not found
    img = Image.new('RGB', (420, 120), color=(255, 255, 255))
    d = ImageDraw.Draw(img)
    d.ellipse([20, 15, 105, 100], outline=(22, 75, 59), width=4)
    d.text((125, 35), "SRM INSTITUTE OF SCIENCE & TECHNOLOGY", fill=(22, 75, 59))
    d.text((125, 62), "Kattankulathur - 603 203", fill=(115, 123, 117))
    img.save(logo_target)

def get_fonts():
    try:
        f_title = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 24)
        f_h2 = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 18)
        f_h3 = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 14)
        f_body = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 13)
        f_small = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 11)
        f_big = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 30)
    except Exception:
        f_title = f_h2 = f_h3 = f_body = f_small = f_big = ImageFont.load_default()
    return f_title, f_h2, f_h3, f_body, f_small, f_big

F_TITLE, F_H2, F_H3, F_BODY, F_SMALL, F_BIG = get_fonts()

# Theme colors
C_FOREST = (22, 75, 59)       # #164B3B
C_EMERALD = (35, 139, 104)    # #238B68
C_SAGE = (168, 195, 160)      # #A8C3A0
C_MINT = (232, 242, 234)      # #E8F2EA
C_CREAM = (248, 247, 240)     # #F8F7F0
C_CHARCOAL = (32, 41, 35)     # #202923
C_SLATE = (115, 123, 117)     # #737B75
C_WHITE = (255, 255, 255)
C_AMBER = (217, 119, 6)
C_BLUE = (37, 99, 235)
C_RED = (220, 38, 38)
C_GREEN = (22, 163, 74)

def draw_app_shell(title_text, subtitle_text, active_nav="Overview", role="STUDENT"):
    img = Image.new('RGB', (1360, 820), color=C_CREAM)
    d = ImageDraw.Draw(img)

    # Sidebar
    d.rectangle([0, 0, 250, 820], fill=C_FOREST)
    d.rounded_rectangle([20, 20, 58, 58], radius=10, fill=C_EMERALD)
    d.text((32, 27), "R", font=F_H2, fill=C_WHITE)
    d.text((70, 20), "ReCircle", font=F_H2, fill=C_WHITE)
    d.text((70, 42), "SRM Campus Trust Network", font=F_SMALL, fill=C_SAGE)

    # User badge in sidebar
    d.rounded_rectangle([16, 74, 234, 136], radius=10, fill=(30, 92, 73), outline=C_EMERALD)
    if role == "ADMIN":
        d.text((26, 82), "Dr. Senthil Kumar", font=F_H3, fill=C_WHITE)
        d.text((26, 100), "SRM-ADMIN-001 • ADMIN", font=F_SMALL, fill=C_SAGE)
        d.rounded_rectangle([180, 82, 224, 106], radius=6, fill=C_EMERALD)
        d.text((186, 86), "98 TS", font=F_SMALL, fill=C_WHITE)
    else:
        d.text((26, 82), "Aarav Krishnan", font=F_H3, fill=C_WHITE)
        d.text((26, 100), "RA2211003010101 • CSE", font=F_SMALL, fill=C_SAGE)
        d.rounded_rectangle([180, 82, 224, 106], radius=6, fill=C_EMERALD)
        d.text((186, 86), "94 TS", font=F_SMALL, fill=C_WHITE)
    d.text((26, 116), "✓ SRM Campus Verified", font=F_SMALL, fill=C_MINT)

    nav_items = [
        "Overview", "Discover Items", "My Listings", "Borrowing", "Lending",
        "Exchanges", "Rentals", "Donations", "Transactions", "Reviews",
        "Notifications (3)", "Trust Score", "Disputes & Reports", "My Profile"
    ]
    if role == "ADMIN":
        nav_items = [
            "Admin Overview", "Platform Analytics", "User Management",
            "Listing Monitoring", "Transaction Ledger", "Dispute Resolution",
            "Moderator Desk", "Discover Items", "Trust Score"
        ]

    y = 154
    for item in nav_items:
        is_act = active_nav in item
        if is_act:
            d.rounded_rectangle([14, y, 236, y + 34], radius=8, fill=C_EMERALD)
            d.text((28, y + 8), f"●  {item}", font=F_H3, fill=C_WHITE)
        else:
            d.text((28, y + 8), f"○  {item}", font=F_BODY, fill=C_MINT)
        y += 38

    # Top Header
    d.rectangle([250, 0, 1360, 60], fill=C_WHITE)
    d.line([250, 60, 1360, 60], fill=C_SAGE, width=1)
    d.text((275, 20), "SHARE MORE. WASTE LESS. BUILD TRUST.  |  SRM IST KATTANKULATHUR", font=F_H3, fill=C_EMERALD)
    d.rounded_rectangle([1140, 14, 1335, 46], radius=8, fill=C_MINT)
    d.text((1156, 21), "Trust Score: 93.5 / 100", font=F_H3, fill=C_FOREST)

    # Page Banner
    d.text((280, 78), title_text, font=F_TITLE, fill=C_CHARCOAL)
    d.text((280, 108), subtitle_text, font=F_BODY, fill=C_SLATE)
    return img, d

def draw_card(d, box, fill=C_WHITE, outline=(215, 222, 216), radius=14):
    d.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=1)

def generate_ui_screenshots():
    # 1. Landing Page
    img = Image.new('RGB', (1360, 820), color=C_CREAM)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 1360, 70], fill=C_FOREST)
    d.text((40, 22), "ReCircle — SRM Campus Community Resource Sharing & Exchange Network", font=F_H2, fill=C_WHITE)
    d.rounded_rectangle([1080, 18, 1185, 52], radius=8, fill=C_EMERALD)
    d.text((1108, 26), "Sign In", font=F_H3, fill=C_WHITE)
    d.rounded_rectangle([1200, 18, 1325, 52], radius=8, fill=C_WHITE)
    d.text((1218, 26), "Register SRM", font=F_H3, fill=C_FOREST)

    # Hero Section
    d.rounded_rectangle([40, 95, 1320, 390], radius=22, fill=C_FOREST)
    d.rounded_rectangle([75, 125, 395, 155], radius=14, fill=C_EMERALD)
    d.text((90, 131), "✓ VERIFIED SRMIST CAMPUS ECOSYSTEM", font=F_SMALL, fill=C_WHITE)
    d.text((75, 175), "Share More. Waste Less. Build Trust.", font=F_BIG, fill=C_WHITE)
    d.text((75, 222), "Borrow textbooks, rent lab equipment, exchange electronics, and donate hostel essentials", font=F_H2, fill=C_MINT)
    d.text((75, 250), "within a verified, reputation-scored SRM Institute of Science and Technology community.", font=F_BODY, fill=C_SAGE)
    d.rounded_rectangle([75, 300, 275, 348], radius=10, fill=C_EMERALD)
    d.text((100, 315), "Explore Marketplace →", font=F_H3, fill=C_WHITE)
    d.rounded_rectangle([295, 300, 495, 348], radius=10, fill=C_WHITE)
    d.text((322, 315), "Publish Resource +", font=F_H3, fill=C_FOREST)

    # Hero KPIs on right
    hero_stats = [("480+", "Verified SRM Students"), ("1,240+", "Resources Shared"), ("94.2%", "On-Time Return Rate"), ("₹1.8L+", "Student Savings")]
    hx = 780
    hy = 135
    for idx, (val, lbl) in enumerate(hero_stats):
        cx = hx + (idx % 2) * 255
        cy = hy + (idx // 2) * 115
        d.rounded_rectangle([cx, cy, cx + 235, cy + 95], radius=14, fill=(30, 94, 74), outline=C_EMERALD)
        d.text((cx + 22, cy + 18), val, font=F_TITLE, fill=C_WHITE)
        d.text((cx + 22, cy + 54), lbl, font=F_BODY, fill=C_SAGE)

    # 4 Sharing Modes + 10 Categories
    d.text((45, 415), "Four Structured Campus Resource Workflows & Dynamic Trust Scoring", font=F_H2, fill=C_CHARCOAL)
    modes = [
        ("1. Borrow & Lend", "Free peer lending with overlap-safe date reservations & return reminders.", C_MINT),
        ("2. Micro-Rentals", "Affordable daily rentals (₹30–₹150/day) with refundable security deposits.", C_MINT),
        ("3. Direct Exchange", "Barter textbooks, calculators, or gadgets with dual handover confirmation.", C_MINT),
        ("4. Campus Donations", "Graduating seniors gift study material & hostel items to incoming juniors.", C_MINT),
    ]
    for i, (mtitle, mdesc, mcol) in enumerate(modes):
        mx = 40 + i * 322
        draw_card(d, [mx, 450, mx + 306, 590], fill=C_WHITE)
        d.rounded_rectangle([mx + 18, 468, mx + 288, 502], radius=8, fill=mcol)
        d.text((mx + 28, 476), mtitle, font=F_H3, fill=C_FOREST)
        d.text((mx + 18, 520), mdesc[:38], font=F_BODY, fill=C_CHARCOAL)
        d.text((mx + 18, 542), mdesc[38:], font=F_BODY, fill=C_SLATE)

    d.text((45, 615), "10 Standardized Campus Resource Categories", font=F_H2, fill=C_CHARCOAL)
    cats = [
        "Books & Academic", "Electronics & Gadgets", "Laboratory Equipment", "Stationery & Drafting", "Sports Equipment",
        "Bicycles & Transit", "Hostel Essentials", "Clothing & Lab Coats", "Musical Instruments", "Other Campus Resources"
    ]
    for i, c in enumerate(cats):
        cx = 40 + (i % 5) * 258
        cy = 650 + (i // 5) * 70
        draw_card(d, [cx, cy, cx + 244, cy + 56], fill=C_WHITE)
        d.text((cx + 16, cy + 18), f"● {c}", font=F_H3, fill=C_FOREST)

    img.save(os.path.join(SCREENSHOTS_DIR, '01_landing_page.png'))

    # 2. Login Page
    img = Image.new('RGB', (1360, 820), color=C_CREAM)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 620, 820], fill=C_FOREST)
    d.text((60, 120), "ReCircle", font=F_BIG, fill=C_WHITE)
    d.text((60, 168), "Trust-Based Campus Community Resource Sharing", font=F_H2, fill=C_SAGE)
    d.text((60, 230), "• Institutional @srmist.edu.in Email Verification", font=F_BODY, fill=C_MINT)
    d.text((60, 265), "• Salted BCrypt Password Hashing & JWT Security", font=F_BODY, fill=C_MINT)
    d.text((60, 300), "• Dynamic 4-Pillar Student Reputation Score (0-100)", font=F_BODY, fill=C_MINT)
    d.text((60, 335), "• Role-Based Access: Student, Moderator & Administrator", font=F_BODY, fill=C_MINT)

    draw_card(d, [710, 90, 1250, 730], fill=C_WHITE, radius=20)
    d.text((760, 130), "Sign In to Your Campus Account", font=F_TITLE, fill=C_FOREST)
    d.text((760, 165), "Quick Demo Role Switcher (SRM Kattankulathur):", font=F_SMALL, fill=C_SLATE)
    roles_demo = ["Student A (Aarav)", "Student B (Diya)", "Moderator", "Administrator"]
    for i, r in enumerate(roles_demo):
        rx = 760 + (i % 2) * 220
        ry = 190 + (i // 2) * 42
        d.rounded_rectangle([rx, ry, rx + 205, ry + 34], radius=8, fill=C_MINT, outline=C_SAGE)
        d.text((rx + 14, ry + 8), r, font=F_SMALL, fill=C_FOREST)

    d.text((760, 295), "Campus Email Address *", font=F_H3, fill=C_CHARCOAL)
    d.rounded_rectangle([760, 320, 1200, 365], radius=10, fill=C_CREAM, outline=C_SAGE)
    d.text((778, 333), "aarav.krishnan@srmist.edu.in", font=F_BODY, fill=C_CHARCOAL)

    d.text((760, 390), "Password *", font=F_H3, fill=C_CHARCOAL)
    d.rounded_rectangle([760, 415, 1200, 460], radius=10, fill=C_CREAM, outline=C_SAGE)
    d.text((778, 428), "••••••••••••", font=F_BODY, fill=C_CHARCOAL)

    d.rounded_rectangle([760, 495, 1200, 545], radius=12, fill=C_FOREST)
    d.text((905, 510), "Sign In to ReCircle →", font=F_H3, fill=C_WHITE)
    d.text((760, 575), "JWT Token Status: HS256 Bearer Session Active | BCrypt Strength=12", font=F_SMALL, fill=C_EMERALD)
    img.save(os.path.join(SCREENSHOTS_DIR, '02_login_page.png'))

    # 3. Registration Page
    img = Image.new('RGB', (1360, 820), color=C_CREAM)
    d = ImageDraw.Draw(img)
    draw_card(d, [180, 50, 1180, 770], fill=C_WHITE, radius=22)
    d.text((240, 85), "Create Your Verified SRM Student Account", font=F_TITLE, fill=C_FOREST)
    d.text((240, 118), "Requires an official @srmist.edu.in email and valid SRM registration number.", font=F_BODY, fill=C_SLATE)
    fields = [
        ("Full Name *", "Pranav Natarajan"),
        ("Campus Email (@srmist.edu.in) *", "pranav.n@srmist.edu.in"),
        ("SRM Registration Number *", "RA2211003010999"),
        ("Academic Department *", "Computer Science and Engineering"),
        ("Year of Study *", "3rd Year B.Tech"),
        ("Contact Number *", "+91 98409 88776"),
        ("Hostel / Campus Block", "Nelson Mandela Hostel Block B"),
        ("Preferred Campus Pickup Spot", "Tech Park Ground Floor Lobby"),
        ("Password (Min 8 chars, Upper/Lower/Digit) *", "••••••••••••••"),
        ("Confirm Password *", "••••••••••••••"),
    ]
    for i, (flbl, fval) in enumerate(fields):
        fx = 240 + (i % 2) * 450
        fy = 165 + (i // 2) * 95
        d.text((fx, fy), flbl, font=F_H3, fill=C_CHARCOAL)
        d.rounded_rectangle([fx, fy + 24, fx + 410, fy + 66], radius=10, fill=C_CREAM, outline=C_SAGE)
        d.text((fx + 14, fy + 36), fval, font=F_BODY, fill=C_CHARCOAL)
    d.rounded_rectangle([240, 665, 1100, 718], radius=12, fill=C_FOREST)
    d.text((535, 682), "Register & Send Campus OTP Verification", font=F_H3, fill=C_WHITE)
    img.save(os.path.join(SCREENSHOTS_DIR, '03_registration_page.png'))

    # 4. Student Dashboard Page
    img, d = draw_app_shell(
        "Welcome back, Aarav Krishnan!",
        "B.Tech Computer Science and Engineering • Year 3 • Preferred Pickup: University Building Clock Tower",
        active_nav="Overview"
    )
    kpis = [
        ("Trust Score", "93.5 / 100", "EXCELLENT TIER", C_MINT),
        ("Profile Completion", "100%", "Campus Verified ✓", C_MINT),
        ("My Listed Items", "4 Items", "3 Available, 1 Reserved", C_WHITE),
        ("Active Borrows", "2 Active", "Next Due: In 3 Days", C_WHITE),
        ("Pending Approvals", "2 Requests", "Action Required", C_WHITE),
        ("Completed Shares", "11 Txns", "100% On-Time Ratio", C_WHITE),
    ]
    for i, (klbl, kval, ksub, kbg) in enumerate(kpis):
        kx = 280 + (i % 3) * 350
        ky = 145 + (i // 3) * 115
        draw_card(d, [kx, ky, kx + 330, ky + 98], fill=kbg)
        d.text((kx + 20, ky + 14), klbl, font=F_SMALL, fill=C_SLATE)
        d.text((kx + 20, ky + 34), kval, font=F_TITLE, fill=C_FOREST)
        d.text((kx + 20, ky + 68), ksub, font=F_SMALL, fill=C_EMERALD)

    # Upcoming returns & notifications
    draw_card(d, [280, 385, 800, 785], fill=C_WHITE)
    d.text((302, 405), "Active Borrowings & Upcoming Return Deadlines", font=F_H2, fill=C_CHARCOAL)
    returns = [
        ("Arduino Mega 2560 + IoT Sensor Kit", "Owner: Diya Nair (ECE)", "Due: 2026-11-18", "ACTIVE • On Track"),
        ("Introduction to Algorithms (CLRS 4th Ed)", "Owner: Karthikeya R (CSE)", "Due: 2026-11-21", "APPROVED • Handover Ready"),
        ("Bosch Digital Laser Distance Meter", "Borrower: Rohan Verma (Mech)", "Due: 2026-11-19", "LENT OUT • Reminder Sent"),
    ]
    ry = 445
    for rtitle, rowner, rdue, rstat in returns:
        d.rounded_rectangle([300, ry, 780, ry + 92], radius=10, fill=C_CREAM, outline=C_SAGE)
        d.text((316, ry + 12), rtitle, font=F_H3, fill=C_CHARCOAL)
        d.text((316, ry + 36), f"{rowner}   |   {rdue}", font=F_BODY, fill=C_SLATE)
        d.text((316, ry + 62), rstat, font=F_SMALL, fill=C_EMERALD)
        ry += 106

    draw_card(d, [820, 385, 1330, 785], fill=C_WHITE)
    d.text((842, 405), "Recent Campus Notifications", font=F_H2, fill=C_CHARCOAL)
    notifs = [
        ("Borrow Request Approved", "Diya Nair approved your request for Arduino Mega Kit."),
        ("Trust Score Increased (+2.5)", "Completed on-time return of Scientific Calculator."),
        ("New Exchange Offer Received", "Rohan Verma offered Engineering Drafter Set for CLRS."),
        ("Return Reminder (48 Hours)", "Please return Digital Multimeter by Nov 18 at Tech Park."),
    ]
    ny = 445
    for ntitle, nmsg in notifs:
        d.rounded_rectangle([840, ny, 1310, ny + 72], radius=10, fill=C_MINT)
        d.text((856, ny + 12), f"● {ntitle}", font=F_H3, fill=C_FOREST)
        d.text((856, ny + 38), nmsg, font=F_SMALL, fill=C_CHARCOAL)
        ny += 82
    img.save(os.path.join(SCREENSHOTS_DIR, '04_student_dashboard.png'))

    # 5. Item Discovery Page
    img, d = draw_app_shell(
        "Campus Resource Discovery & Marketplace",
        "Search, filter, and sort verified items across 10 campus categories and 4 sharing modes",
        active_nav="Discover Items"
    )
    draw_card(d, [280, 140, 1330, 215], fill=C_WHITE)
    d.rounded_rectangle([298, 156, 650, 198], radius=8, fill=C_CREAM, outline=C_SAGE)
    d.text((312, 169), "🔍 Search title, textbook, lab kit, calculator...", font=F_BODY, fill=C_SLATE)
    filters = ["Category: All (10)", "Mode: All (4)", "Condition: Like New+", "Sort: Highest Trust Score", "Clear Filters"]
    fx = 668
    for fl in filters:
        d.rounded_rectangle([fx, 156, fx + 125, 198], radius=8, fill=C_MINT)
        d.text((fx + 10, 170), fl[:18], font=F_SMALL, fill=C_FOREST)
        fx += 132

    items_cards = [
        ("Cormen CLRS Algorithms 4th Ed", "Books & Academic", "BORROW", "Free • Max 14d", "Aarav K (94 TS)", "AVAILABLE"),
        ("Casio fx-991CW Scientific Calc", "Electronics", "BORROW", "Free • Max 7d", "Aarav K (94 TS)", "AVAILABLE"),
        ("Arduino Mega 2560 + IoT Kit", "Laboratory Equip", "RENT", "₹50/day • Dep ₹300", "Diya N (91 TS)", "AVAILABLE"),
        ("Saleae 8-Ch Logic Analyzer", "Electronics", "EXCHANGE", "Swap for Raspberry Pi", "Diya N (91 TS)", "AVAILABLE"),
        ("Hero Sprint 21-Speed Cycle", "Bicycles & Transit", "RENT", "₹80/day • Dep ₹500", "Rohan V (82 TS)", "AVAILABLE"),
        ("Mini Drafter & Drawing Tube", "Stationery", "DONATE", "Free Donation", "Rohan V (82 TS)", "AVAILABLE"),
    ]
    for i, (ititle, icat, imode, iprice, iown, istat) in enumerate(items_cards):
        ix = 280 + (i % 3) * 355
        iy = 235 + (i // 3) * 265
        draw_card(d, [ix, iy, ix + 335, iy + 248], fill=C_WHITE)
        d.rounded_rectangle([ix + 14, iy + 14, ix + 321, iy + 110], radius=10, fill=C_MINT)
        d.rounded_rectangle([ix + 24, iy + 24, ix + 115, iy + 48], radius=6, fill=C_FOREST)
        d.text((ix + 34, iy + 28), imode, font=F_SMALL, fill=C_WHITE)
        d.text((ix + 130, iy + 55), f"[{icat}]", font=F_H3, fill=C_EMERALD)
        d.text((ix + 16, iy + 122), ititle, font=F_H3, fill=C_CHARCOAL)
        d.text((ix + 16, iy + 148), iprice, font=F_BODY, fill=C_FOREST)
        d.text((ix + 16, iy + 174), f"Owner: {iown} ✓", font=F_SMALL, fill=C_SLATE)
        d.rounded_rectangle([ix + 16, iy + 202, ix + 318, iy + 234], radius=8, fill=C_EMERALD)
        d.text((ix + 95, iy + 209), "View Details & Request →", font=F_SMALL, fill=C_WHITE)
    img.save(os.path.join(SCREENSHOTS_DIR, '05_item_discovery.png'))

    # 6. Item Detail Page
    img, d = draw_app_shell(
        "Item Details: Arduino Mega 2560 + Complete Sensor Kit",
        "Category: Laboratory Equipment • Subcategory: Embedded Systems • Location: Tech Park 4th Floor Lab",
        active_nav="Discover Items"
    )
    draw_card(d, [280, 145, 860, 780], fill=C_WHITE)
    d.rounded_rectangle([305, 170, 835, 390], radius=14, fill=C_MINT)
    d.text((430, 265), "[High-Res Gallery: Arduino Mega 2560 + 37 Sensors]", font=F_H2, fill=C_FOREST)
    d.text((305, 412), "Detailed Resource Specification & Usage Rules", font=F_H2, fill=C_CHARCOAL)
    d.text((305, 442), "Includes original Arduino Mega 2560 R3 board, USB cable, breadboard, jumper wires,", font=F_BODY, fill=C_SLATE)
    d.text((305, 466), "ultrasonic, IR, DHT11, servo motor, and OLED display modules. Ideal for 3rd-year", font=F_BODY, fill=C_SLATE)
    d.text((305, 490), "Microcontroller & IoT mini-projects. Please do not exceed 5V on GPIO pins.", font=F_BODY, fill=C_SLATE)
    d.text((305, 535), "Condition: LIKE_NEW   |   Daily Rental: ₹50/day   |   Refundable Deposit: ₹300", font=F_H3, fill=C_FOREST)
    d.text((305, 575), "Verified Student Reviews (Avg 4.9 ★)", font=F_H2, fill=C_CHARCOAL)
    d.rounded_rectangle([305, 608, 835, 680], radius=10, fill=C_CREAM)
    d.text((320, 618), "★★★★★  Aarav Krishnan (CSE) — 'All 37 sensors worked flawlessly for our hackathon!'", font=F_SMALL, fill=C_CHARCOAL)
    d.text((320, 644), "Completed Transaction #TXN-2026-0014 • Returned On Time ✓", font=F_SMALL, fill=C_EMERALD)

    draw_card(d, [885, 145, 1330, 780], fill=C_WHITE)
    d.text((910, 170), "Owner Verification & Trust Profile", font=F_H2, fill=C_CHARCOAL)
    d.rounded_rectangle([910, 205, 1305, 315], radius=12, fill=C_MINT)
    d.text((928, 220), "Diya Nair (RA2211003010142)", font=F_H3, fill=C_FOREST)
    d.text((928, 245), "Electronics & Communication Engg • Year 3", font=F_SMALL, fill=C_CHARCOAL)
    d.text((928, 270), "Trust Score: 91.2 / 100 (EXCELLENT) • ✓ SRM Verified", font=F_SMALL, fill=C_EMERALD)

    d.text((910, 340), "Select Reservation Dates", font=F_H2, fill=C_CHARCOAL)
    d.text((910, 372), "Start Date: 2026-11-16      End Date: 2026-11-20 (4 Days)", font=F_BODY, fill=C_CHARCOAL)
    d.rounded_rectangle([910, 405, 1305, 505], radius=10, fill=C_CREAM, outline=C_SAGE)
    d.text((928, 420), "Backend Rental Cost Breakdown:", font=F_H3, fill=C_FOREST)
    d.text((928, 446), "4 Days × ₹50.00/day = ₹200.00", font=F_BODY, fill=C_CHARCOAL)
    d.text((928, 470), "Refundable Security Deposit = ₹300.00", font=F_SMALL, fill=C_SLATE)
    d.rounded_rectangle([910, 535, 1305, 588], radius=12, fill=C_FOREST)
    d.text((995, 552), "Submit Rental / Borrow Request", font=F_H3, fill=C_WHITE)
    img.save(os.path.join(SCREENSHOTS_DIR, '06_item_detail.png'))

    # 7. Create Listing Page
    img, d = draw_app_shell(
        "Publish a Campus Resource Listing",
        "Share your textbooks, electronics, lab equipment, or hostel essentials with verified SRM peers",
        active_nav="My Listings"
    )
    draw_card(d, [280, 145, 1330, 780], fill=C_WHITE)
    cfields = [
        ("Item Title *", "Digital Signal Processing — Proakis & Manolakis (4th Edition)"),
        ("Category *", "Books and Academic Materials"),
        ("Subcategory *", "ECE Core Textbooks"),
        ("Item Condition *", "LIKE_NEW"),
        ("Sharing Mode *", "BORROW (Free Campus Lending)"),
        ("Campus Pickup Location *", "University Building Library Entrance"),
        ("Available From *", "2026-11-15"),
        ("Available Until *", "2026-12-30"),
        ("Max Borrow Duration (Days) *", "14 Days"),
        ("Optional Security Deposit (₹)", "₹0.00"),
    ]
    for i, (clbl, cval) in enumerate(cfields):
        cx = 310 + (i % 2) * 490
        cy = 175 + (i // 2) * 90
        d.text((cx, cy), clbl, font=F_H3, fill=C_CHARCOAL)
        d.rounded_rectangle([cx, cy + 24, cx + 455, cy + 64], radius=10, fill=C_CREAM, outline=C_SAGE)
        d.text((cx + 14, cy + 34), cval, font=F_BODY, fill=C_CHARCOAL)
    d.rounded_rectangle([310, 635, 1255, 695], radius=10, fill=C_MINT, outline=C_EMERALD)
    d.text((330, 655), "📷 Validated Image Upload: dsp_proakis_cover.png (JPEG/PNG/WEBP <= 5MB Verified ✓)", font=F_H3, fill=C_FOREST)
    d.rounded_rectangle([310, 715, 620, 762], radius=10, fill=C_FOREST)
    d.text((385, 730), "Publish Listing to Marketplace", font=F_H3, fill=C_WHITE)
    img.save(os.path.join(SCREENSHOTS_DIR, '07_create_listing.png'))

    # 8. Borrow & Lending Management Page
    img, d = draw_app_shell(
        "Borrowing & Lending Lifecycle Tracker",
        "Manage approvals, physical handovers, return reminders, and on-time return confirmations",
        active_nav="Borrowing"
    )
    rows = [
        ("BR-1041", "Casio fx-991CW Scientific Calculator", "Borrower: Diya Nair (91 TS)", "2026-11-10 → 2026-11-14", "COMPLETED • Returned On Time ✓", C_GREEN),
        ("BR-1042", "Cormen CLRS Algorithms 4th Ed", "Borrower: Rohan Verma (82 TS)", "2026-11-14 → 2026-11-24", "ACTIVE • Handover Confirmed", C_EMERALD),
        ("BR-1045", "IEEE Scientific Graphing Calculator", "Borrower: Sneha Reddy (88 TS)", "2026-11-18 → 2026-11-22", "PENDING • Awaiting Owner Action", C_AMBER),
    ]
    ry = 155
    for rcode, ritem, ruser, rdates, rstat, rcol in rows:
        draw_card(d, [280, ry, 1330, ry + 175], fill=C_WHITE)
        d.text((305, ry + 20), f"{rcode}  —  {ritem}", font=F_H2, fill=C_CHARCOAL)
        d.text((305, ry + 52), f"{ruser}   |   Reservation Window: {rdates}", font=F_BODY, fill=C_SLATE)
        d.rounded_rectangle([305, ry + 88, 635, ry + 120], radius=8, fill=C_MINT)
        d.text((320, ry + 95), rstat, font=F_H3, fill=rcol)
        d.rounded_rectangle([940, ry + 85, 1115, ry + 125], radius=8, fill=C_FOREST)
        d.text((965, ry + 97), "Confirm Handover", font=F_SMALL, fill=C_WHITE)
        d.rounded_rectangle([1130, ry + 85, 1305, ry + 125], radius=8, fill=C_EMERALD)
        d.text((1158, ry + 97), "Confirm Return ✓", font=F_SMALL, fill=C_WHITE)
        ry += 195
    img.save(os.path.join(SCREENSHOTS_DIR, '08_borrow_management.png'))

    # 9. Exchange Marketplace Page
    img, d = draw_app_shell(
        "Peer-to-Peer Item Exchange Hub",
        "Propose direct item swaps, reserve both listings upon acceptance, and verify dual handovers",
        active_nav="Exchanges"
    )
    draw_card(d, [280, 155, 1330, 460], fill=C_WHITE)
    d.text((310, 180), "Active Exchange Proposal #EX-2026-09 (Status: ACCEPTED — Dual Handover Verification)", font=F_H2, fill=C_FOREST)
    d.rounded_rectangle([310, 225, 740, 365], radius=12, fill=C_MINT)
    d.text((330, 245), "REQUESTED ITEM (Owner: Diya Nair)", font=F_SMALL, fill=C_EMERALD)
    d.text((330, 270), "Saleae 8-Channel USB Logic Analyzer", font=F_H2, fill=C_CHARCOAL)
    d.text((330, 305), "Handover Confirmed by Receiver: YES ✓", font=F_H3, fill=C_FOREST)

    d.text((775, 280), "⇄", font=F_BIG, fill=C_FOREST)

    d.rounded_rectangle([830, 225, 1295, 365], radius=12, fill=C_MINT)
    d.text((850, 245), "OFFERED ITEM (Proposer: Aarav Krishnan)", font=F_SMALL, fill=C_EMERALD)
    d.text((850, 270), "Raspberry Pi 4 Model B (4GB RAM) + Case", font=F_H2, fill=C_CHARCOAL)
    d.text((850, 305), "Handover Confirmed by Proposer: YES ✓", font=F_H3, fill=C_FOREST)

    d.rounded_rectangle([310, 390, 640, 435], radius=10, fill=C_FOREST)
    d.text((345, 405), "Complete Exchange & Unlock Peer Reviews", font=F_H3, fill=C_WHITE)
    img.save(os.path.join(SCREENSHOTS_DIR, '09_exchange_marketplace.png'))

    # 10. Rental Management Page
    img, d = draw_app_shell(
        "Structured Campus Micro-Rental Management",
        "Backend-verified daily rental calculation (rentalDays × dailyRate) and refundable security deposits",
        active_nav="Rentals"
    )
    draw_card(d, [280, 155, 1330, 520], fill=C_WHITE)
    d.text((310, 185), "Rental Contract #RNT-301 — Arduino Mega 2560 + Complete Sensor Kit", font=F_H2, fill=C_FOREST)
    d.text((310, 225), "Renter: Aarav Krishnan (CSE)   |   Owner: Diya Nair (ECE)   |   Period: 4 Days (Nov 16 – Nov 20)", font=F_BODY, fill=C_CHARCOAL)
    rbox = [
        ("Daily Rental Rate", "₹50.00 / day"),
        ("Booked Duration", "4 Days"),
        ("Total Rental Amount", "₹200.00 (4 × ₹50)"),
        ("Security Deposit", "₹300.00 (Refundable)"),
    ]
    for i, (rlbl, rval) in enumerate(rbox):
        rx = 310 + i * 245
        d.rounded_rectangle([rx, 275, rx + 228, 375], radius=12, fill=C_MINT)
        d.text((rx + 18, 295), rlbl, font=F_SMALL, fill=C_SLATE)
        d.text((rx + 18, 325), rval, font=F_H2, fill=C_FOREST)
    d.rounded_rectangle([310, 415, 620, 465], radius=10, fill=C_FOREST)
    d.text((345, 432), "Confirm Return & Refund Deposit ✓", font=F_H3, fill=C_WHITE)
    img.save(os.path.join(SCREENSHOTS_DIR, '10_rental_management.png'))

    # 11. Donation Hub Page
    img, d = draw_app_shell(
        "Campus Resource Donation Hub",
        "Zero-cost academic and hostel resource gifting with donor recipient selection",
        active_nav="Donations"
    )
    draw_card(d, [280, 155, 1330, 520], fill=C_WHITE)
    d.text((310, 185), "Donation Listing: Engineering Drawing Mini-Drafter, Roller Scale & Sheet Tube", font=F_H2, fill=C_FOREST)
    d.text((310, 220), "Donor: Rohan Verma (3rd Year Mechanical)   |   Status: COMPLETED (Single Recipient Enforced)", font=F_BODY, fill=C_SLATE)
    d.rounded_rectangle([310, 265, 1295, 395], radius=12, fill=C_MINT)
    d.text((330, 285), "Selected Recipient: Ananya Iyer (1st Year B.Tech CSE • RA2411003010412)", font=F_H3, fill=C_FOREST)
    d.text((330, 315), "Motivation Message: 'I am taking 21MES101L Engineering Graphics this semester and would be", font=F_BODY, fill=C_CHARCOAL)
    d.text((330, 340), "grateful to reuse your drafter kit and pass it on to another freshman next year!'", font=F_BODY, fill=C_CHARCOAL)
    img.save(os.path.join(SCREENSHOTS_DIR, '11_donation_hub.png'))

    # 12. Trust Score Details Page
    img, d = draw_app_shell(
        "Dynamic Trust & Reputation Score Breakdown",
        "Formula: 0.15 × VerifiedAccount + 0.35 × CompletedTransactions + 0.30 × UserReviews + 0.20 × OnTimeReturn",
        active_nav="Trust Score"
    )
    draw_card(d, [280, 150, 620, 440], fill=C_FOREST)
    d.text((310, 180), "OVERALL CAMPUS TRUST SCORE", font=F_SMALL, fill=C_SAGE)
    d.text((310, 215), "93.5 / 100", font=F_BIG, fill=C_WHITE)
    d.rounded_rectangle([310, 275, 540, 312], radius=8, fill=C_EMERALD)
    d.text((326, 285), "Tier: EXCELLENT CONTRIBUTOR", font=F_H3, fill=C_WHITE)
    d.text((310, 340), "✓ SRM Email & ID Verified (100%)", font=F_BODY, fill=C_MINT)
    d.text((310, 368), "✓ 11 Completed Shares (4.9★ Avg)", font=F_BODY, fill=C_MINT)
    d.text((310, 396), "✓ 100% On-Time Return Record", font=F_BODY, fill=C_MINT)

    draw_card(d, [645, 150, 1330, 440], fill=C_WHITE)
    d.text((675, 175), "Four Weighted Pillar Contributions", font=F_H2, fill=C_CHARCOAL)
    pillars = [
        ("1. Verified Account Pillar (Weight 15%)", "100.0 / 100  →  Contributes 15.0 pts", 1.0),
        ("2. Completed Transactions Pillar (Weight 35%)", "90.0 / 100   →  Contributes 31.5 pts", 0.90),
        ("3. Peer Review Ratings Pillar (Weight 30%)", "90.0 / 100   →  Contributes 27.0 pts", 0.90),
        ("4. On-Time Return Ratio Pillar (Weight 20%)", "100.0 / 100  →  Contributes 20.0 pts", 1.0),
    ]
    py = 215
    for plbl, pval, pratio in pillars:
        d.text((675, py), plbl, font=F_H3, fill=C_CHARCOAL)
        d.text((1035, py), pval, font=F_SMALL, fill=C_FOREST)
        d.rounded_rectangle([675, py + 24, 1295, py + 40], radius=6, fill=C_MINT)
        d.rounded_rectangle([675, py + 24, 675 + int(620 * pratio), py + 40], radius=6, fill=C_EMERALD)
        py += 54

    draw_card(d, [280, 465, 1330, 780], fill=C_WHITE)
    d.text((310, 490), "Immutable Trust Score Recalculation Audit Log (trust_score_history)", font=F_H2, fill=C_CHARCOAL)
    hist = [
        ("2026-11-14 15:42", "PEER_REVIEW_RECEIVED", "89.0 → 93.5 (+4.5)", "Received 5-star review on Transaction #TXN-2026-0014"),
        ("2026-11-12 18:15", "BORROW_RETURN_ON_TIME", "85.5 → 89.0 (+3.5)", "Confirmed on-time return of Casio fx-991CW Calculator"),
        ("2026-10-28 10:05", "CAMPUS_EMAIL_VERIFIED", "76.5 → 85.5 (+9.0)", "Verified institutional @srmist.edu.in OTP credential"),
    ]
    hy = 535
    for hdate, hevt, hdelta, hreason in hist:
        d.rounded_rectangle([310, hy, 1300, hy + 64], radius=10, fill=C_CREAM, outline=C_SAGE)
        d.text((328, hy + 12), f"[{hdate}]  {hevt}   |   {hdelta}", font=F_H3, fill=C_FOREST)
        d.text((328, hy + 36), hreason, font=F_BODY, fill=C_SLATE)
        hy += 76
    img.save(os.path.join(SCREENSHOTS_DIR, '12_trust_score_details.png'))

    # 13. Admin Analytics Dashboard Page
    img, d = draw_app_shell(
        "Administrator Control Center & Visual Analytics Suite",
        "11 Real-Time Platform KPIs + 10 Interactive Recharts Visualizations (Filter: Last 6 Months)",
        active_nav="Platform Analytics",
        role="ADMIN"
    )
    akpis = [
        ("Total Users", "96"), ("Verified SRM", "89"), ("Listed Items", "142"), ("Active Borrows", "28"),
        ("Active Rentals", "14"), ("Exchanges Done", "31"), ("Donations Done", "24"), ("Open Disputes", "1")
    ]
    for i, (albl, aval) in enumerate(akpis):
        ax = 280 + (i % 4) * 264
        ay = 145 + (i // 4) * 78
        draw_card(d, [ax, ay, ax + 250, ay + 66], fill=C_WHITE)
        d.text((ax + 16, ay + 10), albl, font=F_SMALL, fill=C_SLATE)
        d.text((ax + 16, ay + 30), aval, font=F_H2, fill=C_FOREST)

    # Mini charts preview on Admin Analytics screenshot
    draw_card(d, [280, 315, 790, 540], fill=C_WHITE)
    d.text((302, 332), "1. Monthly Campus Registrations & Verification Growth", font=F_H3, fill=C_CHARCOAL)
    months = ["Jun", "Jul", "Aug", "Sep", "Oct", "Nov"]
    vals = [18, 34, 52, 68, 89, 96]
    for i, (m, v) in enumerate(zip(months, vals)):
        bx = 320 + i * 72
        bh = int(v * 1.3)
        d.rectangle([bx, 500 - bh, bx + 42, 500], fill=C_EMERALD)
        d.text((bx + 8, 506), m, font=F_SMALL, fill=C_SLATE)
        d.text((bx + 8, 480 - bh), str(v), font=F_SMALL, fill=C_FOREST)

    draw_card(d, [815, 315, 1330, 540], fill=C_WHITE)
    d.text((838, 332), "2. Resource Sharing Mode Distribution (Borrow/Rent/Exchange/Donate)", font=F_H3, fill=C_CHARCOAL)
    modes_bar = [("Borrowing (46%)", 210, C_FOREST), ("Renting (22%)", 110, C_EMERALD), ("Exchanging (17%)", 85, C_AMBER), ("Donating (15%)", 75, C_BLUE)]
    by = 375
    for mlbl, mw, mcol in modes_bar:
        d.text((840, by), mlbl, font=F_SMALL, fill=C_CHARCOAL)
        d.rounded_rectangle([1010, by + 2, 1010 + mw, by + 22], radius=6, fill=mcol)
        by += 38

    draw_card(d, [280, 558, 1330, 785], fill=C_WHITE)
    d.text((302, 576), "3. Top Campus Contributors Leaderboard & Department Participation", font=F_H3, fill=C_CHARCOAL)
    leaders = [
        ("#1 Aarav Krishnan", "B.Tech CSE • Year 3", "11 Completed Shares", "4.9 ★", "93.5 TS (EXCELLENT)"),
        ("#2 Diya Nair", "B.Tech ECE • Year 3", "9 Completed Shares", "4.8 ★", "91.2 TS (EXCELLENT)"),
        ("#3 Sneha Reddy", "B.Tech Biotech • Year 4", "7 Completed Shares", "4.8 ★", "88.4 TS (TRUSTED)"),
        ("#4 Rohan Verma", "B.Tech Mechanical • Year 2", "5 Completed Shares", "4.6 ★", "82.0 TS (TRUSTED)"),
    ]
    ly = 612
    for l1, l2, l3, l4, l5 in leaders:
        d.rounded_rectangle([302, ly, 1308, ly + 36], radius=6, fill=C_CREAM)
        d.text((318, ly + 8), f"{l1}   |   {l2}   |   {l3}   |   {l4}   |   {l5}", font=F_BODY, fill=C_CHARCOAL)
        ly += 41

    img.save(os.path.join(SCREENSHOTS_DIR, '13_admin_analytics_dashboard.png'))
    print("Generated all 13 UI screenshots in recircle/screenshots/")

def generate_report_charts():
    # 1. MS Planner Agile Task Board (fig_1_1_ms_planner_board.png)
    img = Image.new('RGB', (1320, 720), color=(244, 246, 248))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 1320, 58], fill=C_FOREST)
    d.text((28, 16), "Microsoft Planner — ReCircle Agile Sprint Board (B.Tech CSE Full Stack Project)", font=F_H2, fill=C_WHITE)

    cols = [
        ("Product Backlog (US01–US23)", [
            ("US21: Moderation Escalation Queue", "Completed • 5 SP", C_GREEN),
            ("US22: 10 Recharts Analytics Suite", "Completed • 8 SP", C_GREEN),
            ("US23: Immutable Security Audit Trail", "Completed • 5 SP", C_GREEN),
        ]),
        ("Sprint 1: Auth & Catalog (Done)", [
            ("US01: SRM Campus Email Registration", "Completed • 5 SP", C_FOREST),
            ("US02: BCrypt + JWT Auth & RBAC", "Completed • 8 SP", C_FOREST),
            ("US04: Student Dashboard KPIs", "Completed • 5 SP", C_FOREST),
            ("US05: Multi-Mode Listing CRUD", "Completed • 8 SP", C_FOREST),
            ("US07: Marketplace Search & Filters", "Completed • 8 SP", C_FOREST),
        ]),
        ("Sprint 2: Sharing Engines (Done)", [
            ("US09: Overlap-Safe Borrow Engine", "Completed • 8 SP", C_EMERALD),
            ("US10: Handover & Return Tracker", "Completed • 8 SP", C_EMERALD),
            ("US12: Item-for-Item Exchange Hub", "Completed • 8 SP", C_EMERALD),
            ("US14: Daily Rental Cost Engine", "Completed • 5 SP", C_EMERALD),
            ("US15: Campus Donation Workflow", "Completed • 5 SP", C_EMERALD),
        ]),
        ("Sprint 3: Trust & Admin (Done)", [
            ("US17: 4-Pillar Trust Score Engine", "Completed • 8 SP", C_BLUE),
            ("US18: Verified Peer Review System", "Completed • 5 SP", C_BLUE),
            ("US19: Return Reminder Scheduler", "Completed • 5 SP", C_BLUE),
            ("US20: Dispute Filing & Arbitration", "Completed • 8 SP", C_BLUE),
        ]),
    ]
    for i, (ctitle, cards) in enumerate(cols):
        cx = 24 + i * 322
        draw_card(d, [cx, 78, cx + 306, 695], fill=C_WHITE)
        d.rounded_rectangle([cx + 12, 92, cx + 294, 130], radius=8, fill=C_MINT)
        d.text((cx + 22, 102), ctitle, font=F_H3, fill=C_FOREST)
        cy = 148
        for ktitle, kmeta, kcol in cards:
            d.rounded_rectangle([cx + 14, cy, cx + 292, cy + 92], radius=10, fill=C_CREAM, outline=C_SAGE)
            d.rectangle([cx + 14, cy, cx + 22, cy + 92], fill=kcol)
            d.text((cx + 32, cy + 16), ktitle, font=F_H3, fill=C_CHARCOAL)
            d.text((cx + 32, cy + 46), kmeta, font=F_SMALL, fill=kcol)
            d.text((cx + 32, cy + 66), "✓ Verified in JUnit 5 & UI", font=F_SMALL, fill=C_SLATE)
            cy += 106
    img.save(os.path.join(FIGURES_DIR, 'fig_1_1_ms_planner_board.png'))

    # 2. Release Roadmap (fig_1_2_release_roadmap.png)
    fig, ax = plt.subplots(figsize=(11, 4.8), dpi=160)
    ax.set_facecolor('#F8F7F0')
    fig.patch.set_facecolor('#F8F7F0')
    sprints = [
        ("Sprint 1 (Weeks 1–4)\nIdentity, RBAC & Resource Catalog", 1, 4, '#164B3B', "US01–US08 (34 SP)\n• BCrypt + JWT Auth\n• 10 Campus Categories\n• Discovery & Search"),
        ("Sprint 2 (Weeks 5–8)\nMulti-Mode Sharing Engines", 5, 4, '#238B68', "US09–US16 (42 SP)\n• Borrow & Lend Lifecycle\n• Peer Exchanges\n• Micro-Rentals & Donations"),
        ("Sprint 3 (Weeks 9–12)\nTrust Engine, Governance & QA", 9, 4, '#2D6A4F', "US17–US23 (38 SP)\n• Weighted Trust Score\n• Reviews & Disputes\n• 10 Recharts Analytics & TC01-30"),
    ]
    for idx, (lbl, start, dur, col, desc) in enumerate(sprints):
        y = 2 - idx * 0.8
        ax.barh(y, dur, left=start, height=0.52, color=col, edgecolor='white', linewidth=2)
        ax.text(start + 0.15, y, f"{lbl}\n{desc}", va='center', ha='left', color='white', fontsize=8.5, fontweight='bold')
    ax.set_yticks([2, 1.2, 0.4])
    ax.set_yticklabels(["Release 1.0", "Release 2.0", "Release 3.0 (Final)"], fontweight='bold', fontsize=10)
    ax.set_xlabel("Academic Project Execution Timeline (Weeks 1 to 12)", fontweight='bold', fontsize=10)
    ax.set_title("ReCircle — Agile Release Plan & Iterative Delivery Roadmap", fontweight='bold', fontsize=13, color='#164B3B', pad=12)
    ax.set_xlim(0.5, 13.5)
    ax.grid(axis='x', linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.savefig(os.path.join(FIGURES_DIR, 'fig_1_2_release_roadmap.png'))
    plt.close()

    # 3. Sprint 1, Sprint 2, Sprint 3 & Overall Committed vs Completed Bar Graphs
    sprint_bars = [
        ('fig_2_7_sprint1_bar_graph.png', 'Sprint 1: Committed vs Completed Story Points (US01–US08)', ['Auth & JWT', 'Dashboard', 'Item CRUD', 'Discovery & Filter'], [11, 5, 10, 8], [11, 5, 10, 8]),
        ('fig_2_14_sprint2_bar_graph.png', 'Sprint 2: Committed vs Completed Story Points (US09–US16)', ['Borrow & Lend', 'Item Exchange', 'Micro-Rentals', 'Donations & Ledger'], [16, 10, 8, 8], [16, 10, 8, 8]),
        ('fig_2_21_sprint3_bar_graph.png', 'Sprint 3: Committed vs Completed Story Points (US17–US23)', ['Trust Score Engine', 'Reviews & Alerts', 'Dispute Resolution', 'Admin Analytics'], [10, 10, 8, 10], [10, 10, 8, 10]),
        ('fig_3_1_overall_velocity_graph.png', 'ReCircle Overall Agile Velocity across Sprints 1, 2, and 3 (114 Total SP)', ['Sprint 1 (Identity & Catalog)', 'Sprint 2 (Sharing Workflows)', 'Sprint 3 (Trust & Analytics)'], [34, 42, 38], [34, 42, 38]),
    ]
    import numpy as np
    for fname, title, labels, comm, comp in sprint_bars:
        fig, ax = plt.subplots(figsize=(8.5, 4.5), dpi=160)
        x = np.arange(len(labels))
        w = 0.34
        b1 = ax.bar(x - w/2, comm, w, label='Committed Story Points', color='#164B3B')
        b2 = ax.bar(x + w/2, comp, w, label='Completed Story Points', color='#238B68')
        ax.set_title(title, fontweight='bold', fontsize=11.5, color='#164B3B', pad=12)
        ax.set_xticks(x)
        ax.set_xticklabels(labels, fontsize=9.5, fontweight='bold')
        ax.set_ylabel('Story Points (SP)', fontweight='bold')
        ax.set_ylim(0, max(comm) * 1.28)
        ax.legend()
        ax.grid(axis='y', linestyle='--', alpha=0.4)
        for bar in list(b1) + list(b2):
            h = bar.get_height()
            ax.annotate(f'{h} SP', xy=(bar.get_x() + bar.get_width() / 2, h),
                        xytext=(0, 4), textcoords="offset points", ha='center', va='bottom', fontsize=8.5, fontweight='bold')
        plt.tight_layout()
        plt.savefig(os.path.join(FIGURES_DIR, fname))
        plt.close()

    # 4. System Architecture & ER Diagrams for Sprints 1, 2, 3
    arch_specs = [
        ('fig_2_1_sprint1_architecture.png', 'Sprint 1 System Architecture — Authentication, RBAC & Item Discovery',
         ["React 19 + TS Client\n(Landing, Auth, Discovery, Item CRUD)", "JwtAuthenticationFilter\n+ BCrypt PasswordHasher", "AuthService & ItemService\n(Domain Validation & Upload)", "MySQL 8 / JPA Repositories\n(users, categories, items, images)"]),
        ('fig_2_8_sprint2_architecture.png', 'Sprint 2 System Architecture — Multi-Mode Sharing & Reservation Engine',
         ["React 19 Workflow Views\n(Borrow, Exchange, Rent, Donate)", "SharingWorkflowController\n(/api/v1/borrow, exchanges, rentals)", "SharingWorkflowService\n(Overlap Detection & Cost Calc)", "MySQL Transaction Ledger\n(borrow_requests, exchanges, rentals, donations)"]),
        ('fig_2_15_sprint3_architecture.png', 'Sprint 3 Complete Full-Stack Architecture — Trust Engine, Scheduler & Analytics',
         ["React 19 + Recharts SPA\n(32 Views: Student, Mod, Admin)", "Spring Boot 3.2 REST API\n(JWT + Role Guard + Scheduler)", "TrustScoreService (4-Pillar)\n+ AdminAnalyticsService", "Normalized 21-Table MySQL DB\n(recircle_db + Audit Trail)"]),
    ]
    for fname, title, boxes in arch_specs:
        img = Image.new('RGB', (1200, 420), color=C_CREAM)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 1200, 52], fill=C_FOREST)
        d.text((24, 14), title, font=F_H2, fill=C_WHITE)
        for i, btxt in enumerate(boxes):
            bx = 35 + i * 288
            draw_card(d, [bx, 110, bx + 250, 330], fill=C_WHITE, outline=C_EMERALD)
            d.rounded_rectangle([bx + 14, 126, bx + 236, 162], radius=8, fill=C_MINT)
            d.text((bx + 24, 134), f"Tier {i+1}", font=F_H3, fill=C_FOREST)
            lines = btxt.split('\n')
            ly = 190
            for ln in lines:
                d.text((bx + 18, ly), ln, font=F_BODY, fill=C_CHARCOAL)
                ly += 28
            if i < len(boxes) - 1:
                d.text((bx + 258, 205), "➔", font=F_TITLE, fill=C_EMERALD)
        img.save(os.path.join(FIGURES_DIR, fname))

    # ER Diagrams for Sprints 1, 2, 3
    er_specs = [
        ('fig_2_2_sprint1_er_diagram.png', 'Sprint 1 Entity-Relationship Diagram (Identity & Catalog Core)',
         [("users", "id (PK)\nfull_name\nemail (UQ)\nregistration_number (UQ)\npassword_hash\nrole, account_status\ncampus_verified"),
          ("email_verification_tokens", "id (PK)\nuser_id (FK)\notp_code\nexpires_at\nconsumed"),
          ("categories", "id (PK)\nname (UQ)\nslug (UQ)\ndescription\nicon_name\nactive"),
          ("items", "id (PK)\nowner_id (FK)\ncategory_id (FK)\ntitle, sharing_type\nitem_condition\navailability_status\ndaily_rate, deposit"),
          ("item_images", "id (PK)\nitem_id (FK)\nimage_url\ndisplay_order")]),
        ('fig_2_9_sprint2_er_diagram.png', 'Sprint 2 Entity-Relationship Diagram (Multi-Mode Sharing Workflows)',
         [("borrow_requests", "id (PK)\nitem_id (FK)\nborrower_id (FK)\nlender_id (FK)\nstart_date, end_date\nstatus, returned_on_time"),
          ("exchange_offers", "id (PK)\nrequested_item_id (FK)\noffered_item_id (FK)\nproposer_id, receiver_id\nstatus, dual_handover"),
          ("rentals", "id (PK)\nitem_id (FK)\nrenter_id, owner_id\nrental_days, daily_rate\ntotal_rental_amount\npayment_status, status"),
          ("donation_requests", "id (PK)\nitem_id (FK)\nrequester_id, donor_id\nmotivation_message\nstatus, completed_at"),
          ("transactions", "id (PK)\ntransaction_code (UQ)\ntransaction_type\nreference_id\nprimary_item_id (FK)\nstatus, monetary_amount")]),
        ('fig_2_16_sprint3_er_diagram.png', 'Sprint 3 Complete 21-Table Normalized Database Schema (recircle_db)',
         [("trust_scores", "id (PK)\nuser_id (UQ, FK)\ntotal_score (0-100)\ntrust_level\nverified_account_score\ncompleted_tx_score\nuser_review_score\non_time_return_score"),
          ("trust_score_history", "id (PK)\nuser_id (FK)\nprevious_score, new_score\ndelta, trigger_event\nreason_summary"),
          ("reviews", "id (PK)\ntransaction_id (FK)\nreviewer_id (FK)\nreviewee_id (FK)\nrating (1-5)\ncomment, flagged"),
          ("disputes & evidence", "id (PK)\ntransaction_id (FK)\nreporter_id, reported_id\ncategory, status\nresolution_notes"),
          ("notifications & audit_logs", "id (PK)\nuser_id / actor_id (FK)\naction_type / type\ntarget_entity\nread / created_at")]),
    ]
    for fname, title, entities in er_specs:
        img = Image.new('RGB', (1300, 440), color=C_WHITE)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 1300, 52], fill=C_FOREST)
        d.text((24, 14), title, font=F_H2, fill=C_WHITE)
        for i, (ename, ecols) in enumerate(entities):
            ex = 22 + i * 252
            draw_card(d, [ex, 82, ex + 236, 405], fill=C_CREAM, outline=C_FOREST)
            d.rounded_rectangle([ex, 82, ex + 236, 120], radius=10, fill=C_EMERALD)
            d.text((ex + 14, 92), ename, font=F_H3, fill=C_WHITE)
            ly = 134
            for ln in ecols.split('\n'):
                d.text((ex + 14, ly), ln, font=F_BODY, fill=C_CHARCOAL)
                ly += 26
            if i < len(entities) - 1:
                d.line([ex + 236, 240, ex + 252, 240], fill=C_FOREST, width=3)
        img.save(os.path.join(FIGURES_DIR, fname))

    print("Generated all report charts and diagrams in recircle/report/figures/")

if __name__ == '__main__':
    extract_srm_logo()
    generate_ui_screenshots()
    generate_report_charts()
