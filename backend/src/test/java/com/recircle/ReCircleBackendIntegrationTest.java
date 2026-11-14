package com.recircle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recircle.dto.ApiDtos.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:recircle_test_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@org.springframework.test.context.TestExecutionListeners(
    listeners = {
        org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
        org.springframework.test.context.web.ServletTestExecutionListener.class
    },
    mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReCircleBackendIntegrationTest {

    @Autowired(required = false)
    private MockMvc mockMvc;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    private static String studentToken;
    private static String secondStudentToken;
    private static String adminToken;
    private static Long registeredStudentId;
    private static Long createdBorrowItemId;
    private static Long createdRentItemId;
    private static Long createdDonateItemId;
    private static Long createdBorrowRequestId;
    private static Long completedBorrowTxId;

    @Test
    @Order(1)
    @DisplayName("TC01: Valid Student Registration with SRM Campus Email")
    void test01_validRegistration() throws Exception {
        RegisterRequest req = new RegisterRequest(
            "Pranav Natarajan",
            "pranav.n@srmist.edu.in",
            "SecurePass@2026",
            "SecurePass@2026",
            "RA2211003010999",
            "Computer Science and Engineering",
            3,
            "+91 98409 88776",
            "Nelson Mandela Hostel",
            "CSE Junior interested in AI and hardware sharing.",
            "Tech Park Entrance",
            null
        );

        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("pranav.n@srmist.edu.in"))
            .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        studentToken = json.get("accessToken").asText();
        registeredStudentId = json.get("user").get("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("TC02: Invalid Non-Campus Email Rejection")
    void test02_invalidEmailDomain() throws Exception {
        RegisterRequest req = new RegisterRequest(
            "External User",
            "external@gmail.com",
            "SecurePass@2026",
            "SecurePass@2026",
            "RA2211003010888",
            "CSE",
            2,
            "9876543210",
            null, null, null, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_CAMPUS_DOMAIN"));
    }

    @Test
    @Order(3)
    @DisplayName("TC03: Duplicate Registration Prevention")
    void test03_duplicateRegistration() throws Exception {
        RegisterRequest req = new RegisterRequest(
            "Pranav Duplicate",
            "pranav.n@srmist.edu.in",
            "SecurePass@2026",
            "SecurePass@2026",
            "RA2211003010999",
            "CSE",
            3,
            "9876543210",
            null, null, null, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));
    }

    @Test
    @Order(4)
    @DisplayName("TC04: Weak Password Validation")
    void test04_passwordValidation() throws Exception {
        RegisterRequest req = new RegisterRequest(
            "Weak Pass User",
            "weak@srmist.edu.in",
            "short",
            "short",
            "RA2211003010777",
            "CSE",
            2,
            "9876543210",
            null, null, null, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("WEAK_PASSWORD"));
    }

    @Test
    @Order(5)
    @DisplayName("TC05: Valid Login for Seeded Student & Admin")
    void test05_validLogin() throws Exception {
        MvcResult sRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("diya.s@srmist.edu.in", "ReCircle@2026"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();
        secondStudentToken = objectMapper.readTree(sRes.getResponse().getContentAsString()).get("accessToken").asText();

        MvcResult aRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin@srmist.edu.in", "ReCircle@2026"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andReturn();
        adminToken = objectMapper.readTree(aRes.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @Order(6)
    @DisplayName("TC06: Invalid Login Credentials Rejected")
    void test06_invalidLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("diya.s@srmist.edu.in", "WrongPassword123"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("TC07: Unauthorized Request Without JWT Blocked")
    void test07_unauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("TC08: Role-Based Access Control (Student Blocked from Admin Endpoints)")
    void test08_roleBasedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("TC09: Item Creation Across Borrow, Rent, and Donate")
    void test09_itemCreation() throws Exception {
        ItemUpsertRequest borrowItem = new ItemUpsertRequest(
            "Digital Signal Processing Proakis 4th Ed",
            "Clean textbook for ECE 5th semester DSP course.",
            1L, "ECE Textbooks", "LIKE_NEW", "BORROW", "AVAILABLE",
            "Tech Park Library", BigDecimal.ZERO, BigDecimal.ZERO,
            LocalDate.now().toString(), LocalDate.now().plusMonths(3).toString(),
            10, null, "Handle with care", null, List.of()
        );

        MvcResult res1 = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(borrowItem)))
            .andExpect(status().isCreated())
            .andReturn();
        createdBorrowItemId = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        ItemUpsertRequest rentItem = new ItemUpsertRequest(
            "Raspberry Pi 5 8GB Kit with Active Cooler",
            "Complete single-board computer kit for final year project prototyping.",
            2L, "Embedded Kits", "NEW", "RENT", "AVAILABLE",
            "Tech Park 5th Floor", new BigDecimal("500.00"), new BigDecimal("50.00"),
            LocalDate.now().toString(), LocalDate.now().plusMonths(3).toString(),
            14, null, "Do not overclock", null, List.of()
        );

        MvcResult res2 = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rentItem)))
            .andExpect(status().isCreated())
            .andReturn();
        createdRentItemId = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        ItemUpsertRequest donateItem = new ItemUpsertRequest(
            "GATE CSE Previous Year Solved Papers Book",
            "Free donation for juniors preparing for GATE.",
            1L, "Exam Prep", "GOOD", "DONATE", "AVAILABLE",
            "Nelson Mandela Hostel", BigDecimal.ZERO, BigDecimal.ZERO,
            LocalDate.now().toString(), LocalDate.now().plusMonths(6).toString(),
            365, null, "Pass it on after GATE exam", null, List.of()
        );

        MvcResult res3 = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(donateItem)))
            .andExpect(status().isCreated())
            .andReturn();
        createdDonateItemId = objectMapper.readTree(res3.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @Order(10)
    @DisplayName("TC10: Item Editing by Owner & Unauthorized Edit Prevention")
    void test10_itemEditing() throws Exception {
        ItemUpsertRequest editReq = new ItemUpsertRequest(
            "Digital Signal Processing Proakis (Updated Edition)",
            "Updated description with solved tutorial manual included.",
            1L, "ECE Textbooks", "LIKE_NEW", "BORROW", "AVAILABLE",
            "Tech Park Library", BigDecimal.ZERO, BigDecimal.ZERO,
            LocalDate.now().toString(), LocalDate.now().plusMonths(3).toString(),
            12, null, "Handle with care", null, List.of()
        );

        mockMvc.perform(put("/api/v1/items/" + createdBorrowItemId)
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(editReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Digital Signal Processing Proakis (Updated Edition)"));

        // TC30: Unauthorized modification by another student blocked
        mockMvc.perform(put("/api/v1/items/" + createdBorrowItemId)
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(editReq)))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("TC11 & TC12: Search, Filtering, and Item Archival")
    void test11_searchFilterAndArchive() throws Exception {
        mockMvc.perform(get("/api/v1/items")
                .param("query", "Proakis")
                .param("sharingType", "BORROW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));

        // Create temporary item and archive it
        ItemUpsertRequest temp = new ItemUpsertRequest(
            "Temporary Archive Test Item", "Desc", 4L, "Stationery", "GOOD", "BORROW",
            "AVAILABLE", "Clock Tower", BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, 7, null, null, null, List.of()
        );
        MvcResult r = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(temp)))
            .andExpect(status().isCreated())
            .andReturn();
        Long tempId = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/items/" + tempId)
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk());

        // Archived item must not appear in ordinary search results
        mockMvc.perform(get("/api/v1/items").param("query", "Temporary Archive Test Item"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(12)
    @DisplayName("TC13, TC14, TC15, TC16, TC17, TC29: Borrow Request Lifecycle & Conflict Prevention")
    void test12_borrowLifecycleAndConflictPrevention() throws Exception {
        String start = LocalDate.now().plusDays(1).toString();
        String end = LocalDate.now().plusDays(5).toString();

        // Self-request forbidden
        BorrowCreateRequest selfReq = new BorrowCreateRequest(createdBorrowItemId, start, end, "Self test");
        mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selfReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("SELF_REQUEST_FORBIDDEN"));

        // Valid borrow request by secondStudent
        BorrowCreateRequest validReq = new BorrowCreateRequest(createdBorrowItemId, start, end, "DSP exam preparation");
        MvcResult bRes = mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        createdBorrowRequestId = objectMapper.readTree(bRes.getResponse().getContentAsString()).get("id").asLong();

        // Owner approves borrow request
        mockMvc.perform(put("/api/v1/borrow-requests/" + createdBorrowRequestId + "/approve")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DecisionNoteRequest("Approved! Meet at Tech Park."))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        // TC14 & TC29: Concurrent/Overlapping reservation attempt during reserved dates must be rejected with 409 CONFLICT
        mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReq)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("OVERLAPPING_RESERVATION"));

        // Rule 3.A: Non-overlapping future dates on the SAME item MUST succeed and be bookable!
        String nonOverlapStart = LocalDate.now().plusDays(20).toString();
        String nonOverlapEnd = LocalDate.now().plusDays(23).toString();
        BorrowCreateRequest nonOverlapReq = new BorrowCreateRequest(createdBorrowItemId, nonOverlapStart, nonOverlapEnd, "Second non-overlapping booking");
        MvcResult nonOverlapRes = mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonOverlapReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        Long secondBookingId = objectMapper.readTree(nonOverlapRes.getResponse().getContentAsString()).get("id").asLong();

        // Cancel the second non-overlapping booking to test cancellation workflow (Rule 3.D)
        mockMvc.perform(put("/api/v1/borrow-requests/" + secondBookingId + "/cancel")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Handover confirmation -> ACTIVE
        mockMvc.perform(put("/api/v1/borrow-requests/" + createdBorrowRequestId + "/confirm-handover")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Return request -> RETURN_REQUESTED
        mockMvc.perform(put("/api/v1/borrow-requests/" + createdBorrowRequestId + "/request-return")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETURN_REQUESTED"));

        // Owner confirms return -> COMPLETED
        MvcResult compRes = mockMvc.perform(put("/api/v1/borrow-requests/" + createdBorrowRequestId + "/confirm-return")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andReturn();
        completedBorrowTxId = objectMapper.readTree(compRes.getResponse().getContentAsString()).get("transactionId").asLong();
    }

    @Test
    @Order(13)
    @DisplayName("TC18: Exchange Proposal, Acceptance & Dual Handover Completion")
    void test13_exchangeWorkflow() throws Exception {
        // Seeded exchange offer #1 exists between Diya (u2, secondStudentToken) and Aarav (u1)
        MvcResult loginAarav = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("aarav.krishnan@srmist.edu.in", "ReCircle@2026"))))
            .andExpect(status().isOk())
            .andReturn();
        String aaravToken = objectMapper.readTree(loginAarav.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(put("/api/v1/exchanges/1/accept")
                .header("Authorization", "Bearer " + aaravToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Unilateral /complete before counterparty confirms handover MUST be rejected (US11 Dual Handover Rule)
        mockMvc.perform(put("/api/v1/exchanges/1/complete")
                .header("Authorization", "Bearer " + aaravToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("DUAL_HANDOVER_REQUIRED"));

        // First participant (Aarav) confirms handover -> HANDOVER_PENDING
        mockMvc.perform(put("/api/v1/exchanges/1/confirm-handover")
                .header("Authorization", "Bearer " + aaravToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("HANDOVER_PENDING"))
            .andExpect(jsonPath("$.receiverHandoverConfirmed").value(true))
            .andExpect(jsonPath("$.proposerHandoverConfirmed").value(false));

        // Second participant (Diya) confirms handover -> COMPLETED
        mockMvc.perform(put("/api/v1/exchanges/1/confirm-handover")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.proposerHandoverConfirmed").value(true))
            .andExpect(jsonPath("$.receiverHandoverConfirmed").value(true));
    }

    @Test
    @Order(14)
    @DisplayName("TC19: Backend Rental Cost Calculation (4 Days * ₹50 = ₹200)")
    void test14_rentalCostCalculation() throws Exception {
        String start = LocalDate.now().plusDays(2).toString();
        String end = LocalDate.now().plusDays(6).toString(); // 4 days difference
        RentalCreateRequest rentReq = new RentalCreateRequest(createdRentItemId, start, end, true);

        MvcResult rRes = mockMvc.perform(post("/api/v1/rentals")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rentReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rentalDays").value(4))
            .andExpect(jsonPath("$.totalRentalAmount").value(200.00))
            .andExpect(jsonPath("$.paymentStatus").value("PAID_SIMULATED"))
            .andReturn();

        Long rentalId = objectMapper.readTree(rRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/rentals/" + rentalId + "/approve")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @Order(15)
    @DisplayName("TC20: Donation Request Approval & Single-Recipient Enforcement")
    void test15_donationApproval() throws Exception {
        DonationCreateRequest dReq = new DonationCreateRequest(createdDonateItemId, "Preparing for GATE 2027 CSE.");
        MvcResult dRes = mockMvc.perform(post("/api/v1/donations")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dReq)))
            .andExpect(status().isCreated())
            .andReturn();
        Long donationReqId = objectMapper.readTree(dRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/donations/requests/" + donationReqId + "/approve")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        // Second donation request for already-assigned donation must fail with 409 CONFLICT
        mockMvc.perform(post("/api/v1/donations")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dReq)))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(16)
    @DisplayName("TC21 & TC22: Review Eligibility, Duplicate Review Prevention & Trust Score Recalculation")
    void test16_reviewsAndTrustScore() throws Exception {
        // Verify email first using dev OTP 123456
        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest("pranav.n@srmist.edu.in", "123456"))))
            .andExpect(status().isOk());

        // Submit valid review on completed borrow transaction
        ReviewCreateRequest revReq = new ReviewCreateRequest(completedBorrowTxId, 5, "Excellent textbook and smooth handover!");
        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rating").value(5));

        // Duplicate review by same user on same transaction must be blocked (409 CONFLICT)
        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revReq)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("DUPLICATE_REVIEW"));

        // Verify Trust Score recalculated & history recorded
        MvcResult tsRes = mockMvc.perform(get("/api/v1/users/" + registeredStudentId + "/trust-score"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verifiedAccountScore").value(100.00))
            .andExpect(jsonPath("$.userReviewScore").value(100.00))
            .andReturn();
        JsonNode tsNode = objectMapper.readTree(tsRes.getResponse().getContentAsString());
        assertThat(tsNode.get("history").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(17)
    @DisplayName("TC23, TC24, TC25, TC26, TC27: Notifications, Disputes, Image Upload Validation & Admin Analytics")
    void test17_notificationsDisputesUploadsAndAdmin() throws Exception {
        // TC23: Notifications
        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifications").isArray());

        mockMvc.perform(put("/api/v1/notifications/read-all")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk());

        // TC24: Dispute creation & Admin resolution
        DisputeCreateRequest dispReq = new DisputeCreateRequest(
            completedBorrowTxId, createdBorrowItemId, registeredStudentId,
            "DAMAGED_ITEM", "Minor page crease noticed", "Reporting minor crease on page 42.",
            "Attached photo inspection note", null
        );
        MvcResult dispRes = mockMvc.perform(post("/api/v1/disputes")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dispReq)))
            .andExpect(status().isCreated())
            .andReturn();
        Long dispId = objectMapper.readTree(dispRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/disputes/" + dispId + "/resolve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DisputeResolveRequest("RESOLVED", "Verified minor crease pre-existed; resolved amicably."))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"));

        // TC25 & TC26: Admin Dashboard KPIs & Analytics
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRegisteredUsers").isNumber());

        mockMvc.perform(get("/api/v1/admin/analytics")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryDistribution").isArray())
            .andExpect(jsonPath("$.trustDistribution").isArray());

        // TC27: Image Upload Validation (reject invalid MIME type, accept valid PNG)
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "script.exe", "application/octet-stream", new byte[]{1, 2, 3}
        );
        mockMvc.perform(multipart("/api/v1/items/upload-image")
                .file(invalidFile)
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_FILE_TYPE"));

        MockMultipartFile validPng = new MockMultipartFile(
            "file", "item.png", "image/png", new byte[]{(byte) 137, 80, 78, 71}
        );
        mockMvc.perform(multipart("/api/v1/items/upload-image")
                .file(validPng)
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.imageUrl").isNotEmpty());
    }

    @Test
    @Order(18)
    @DisplayName("TC28: Health Check, Date-Range Availability, Payment Integrity (PENDING on Approval), Single-Use OTP & Dispute Penalty")
    void test18_healthDateAvailabilityPaymentAndDisputePenalty() throws Exception {
        // 1. Phase 0 #10: Backend Health-Check Endpoint
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("recircle-backend"));

        // 2. US03: Single-use OTP cannot be reused after verification
        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest("pranav.n@srmist.edu.in", "123456"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_OTP"));

        // 3. Rule 3.B: Rental without simulated payment stays PENDING even after owner approval
        String rStart = LocalDate.now().plusDays(30).toString();
        String rEnd = LocalDate.now().plusDays(33).toString();
        RentalCreateRequest unpaidRentReq = new RentalCreateRequest(createdRentItemId, rStart, rEnd, false);
        MvcResult rRes = mockMvc.perform(post("/api/v1/rentals")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unpaidRentReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
            .andReturn();
        Long unpaidRentalId = objectMapper.readTree(rRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/rentals/" + unpaidRentalId + "/approve")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        // 4. Rule 3.A: Date-based availability endpoint reflects booked dates vs free dates
        mockMvc.perform(get("/api/v1/items/" + createdRentItemId + "/availability")
                .param("startDate", rStart)
                .param("endDate", rEnd))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableForRequestedDates").value(false));

        mockMvc.perform(get("/api/v1/items/" + createdRentItemId + "/availability")
                .param("startDate", LocalDate.now().plusDays(45).toString())
                .param("endDate", LocalDate.now().plusDays(48).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableForRequestedDates").value(true));

        // 5. Rule 3.C: Resolved dispute with penalizeReportedUser=true deducts 15.00 points and logs history
        MvcResult beforeTs = mockMvc.perform(get("/api/v1/users/" + registeredStudentId + "/trust-score"))
            .andExpect(status().isOk())
            .andReturn();
        double scoreBefore = objectMapper.readTree(beforeTs.getResponse().getContentAsString()).get("totalScore").asDouble();

        DisputeCreateRequest penaltyDispReq = new DisputeCreateRequest(
            completedBorrowTxId, createdBorrowItemId, registeredStudentId,
            "POLICY_VIOLATION", "Confirmed late handover violation", "Confirmed policy breach during exchange.",
            "Moderator verified evidence", null
        );
        MvcResult dRes = mockMvc.perform(post("/api/v1/disputes")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(penaltyDispReq)))
            .andExpect(status().isCreated())
            .andReturn();
        Long penaltyDispId = objectMapper.readTree(dRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/disputes/" + penaltyDispId + "/resolve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DisputeResolveRequest("RESOLVED", "Confirmed violation; applying trust penalty.", true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"));

        MvcResult afterTs = mockMvc.perform(get("/api/v1/users/" + registeredStudentId + "/trust-score"))
            .andExpect(status().isOk())
            .andReturn();
        double scoreAfter = objectMapper.readTree(afterTs.getResponse().getContentAsString()).get("totalScore").asDouble();
        assertThat(scoreBefore - scoreAfter).isEqualTo(15.0);
    }

    @Test
    @Order(19)
    @DisplayName("TC31 (Sprint 2): Borrow Rejection, Unauthorized Approvals, Details Endpoint & Late Return Calculation")
    void test19_sprint2BorrowRejectionUnauthorizedAndLateReturn() throws Exception {
        // Login third student (Aarav) to test unauthorized access
        MvcResult loginAarav = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("aarav.krishnan@srmist.edu.in", "ReCircle@2026"))))
            .andExpect(status().isOk())
            .andReturn();
        String aaravToken = objectMapper.readTree(loginAarav.getResponse().getContentAsString()).get("accessToken").asText();

        // 1. Create a borrow request to test unauthorized approval & owner rejection
        String s1 = LocalDate.now().plusDays(25).toString();
        String e1 = LocalDate.now().plusDays(28).toString();
        BorrowCreateRequest req1 = new BorrowCreateRequest(createdBorrowItemId, s1, e1, "Request to test rejection");
        MvcResult r1 = mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
            .andExpect(status().isCreated())
            .andReturn();
        Long rejBorrowId = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();

        // Duplicate pending request by same student on overlapping dates must be blocked (409 DUPLICATE_BORROW_REQUEST)
        mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("DUPLICATE_BORROW_REQUEST"));

        // Unauthorized approval by non-owner (secondStudent or Aarav) MUST be blocked with 403 FORBIDDEN
        mockMvc.perform(put("/api/v1/borrow-requests/" + rejBorrowId + "/approve")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isForbidden());

        // Borrow Request Details endpoint (GET /api/v1/borrow-requests/{id}) accessible to participant, forbidden to outsider
        mockMvc.perform(get("/api/v1/borrow-requests/" + rejBorrowId)
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(rejBorrowId));

        mockMvc.perform(get("/api/v1/borrow-requests/" + rejBorrowId)
                .header("Authorization", "Bearer " + aaravToken))
            .andExpect(status().isForbidden());

        // Owner rejects the borrow request with decision notes
        mockMvc.perform(put("/api/v1/borrow-requests/" + rejBorrowId + "/reject")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DecisionNoteRequest("Need textbook for my own quiz that week."))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.ownerResponseNote").value("Need textbook for my own quiz that week."));

        // 2. Late Return Calculation (US10): create borrow request whose endDate is yesterday
        String lateStart = LocalDate.now().minusDays(4).toString();
        String lateEnd = LocalDate.now().minusDays(1).toString();
        BorrowCreateRequest lateReq = new BorrowCreateRequest(createdBorrowItemId, lateStart, lateEnd, "Late return test");
        MvcResult lateRes = mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lateReq)))
            .andExpect(status().isCreated())
            .andReturn();
        Long lateBorrowId = objectMapper.readTree(lateRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/borrow-requests/" + lateBorrowId + "/approve")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(put("/api/v1/borrow-requests/" + lateBorrowId + "/confirm-handover")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/v1/borrow-requests/" + lateBorrowId + "/request-return")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETURN_REQUESTED"));

        mockMvc.perform(put("/api/v1/borrow-requests/" + lateBorrowId + "/confirm-return")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.returnedOnTime").value(false))
            .andExpect(jsonPath("$.returnedAt").isNotEmpty());
    }

    @Test
    @Order(20)
    @DisplayName("TC32 (Sprint 2): Exchange Proposal Creation, Competing Offer Rejection & Cancellation")
    void test20_sprint2ExchangeProposalAndCompetingRejection() throws Exception {
        // Create two fresh EXCHANGE items: one by studentToken, one by secondStudentToken
        ItemUpsertRequest itemA = new ItemUpsertRequest(
            "Arduino Mega 2560 Kit for Swap", "Full microcontroller kit with jumper wires.",
            2L, "Microcontrollers", "LIKE_NEW", "EXCHANGE", "AVAILABLE",
            "Tech Park Lab 402", BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, 14, "Raspberry Pi Pico or Logic Analyzer", "Direct swap", null, List.of()
        );
        MvcResult resA = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemA)))
            .andExpect(status().isCreated())
            .andReturn();
        Long itemAId = objectMapper.readTree(resA.getResponse().getContentAsString()).get("id").asLong();

        ItemUpsertRequest itemB = new ItemUpsertRequest(
            "Saleae 8-Channel Logic Analyzer", "USB logic analyzer for embedded debugging.",
            2L, "Test Equipment", "GOOD", "EXCHANGE", "AVAILABLE",
            "Main Campus Clock Tower", BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, 14, "Arduino Mega 2560 Kit", "Direct swap", null, List.of()
        );
        MvcResult resB = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemB)))
            .andExpect(status().isCreated())
            .andReturn();
        Long itemBId = objectMapper.readTree(resB.getResponse().getContentAsString()).get("id").asLong();

        // Propose an exchange and cancel it first
        ExchangeCreateRequest cancelProp = new ExchangeCreateRequest(itemAId, itemBId, "Initial proposal to test cancel");
        MvcResult cancelRes = mockMvc.perform(post("/api/v1/exchanges")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelProp)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        Long cancelExId = objectMapper.readTree(cancelRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/exchanges/" + cancelExId + "/cancel")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Now propose a new exchange and complete it via dual handover confirmation
        ExchangeCreateRequest activeProp = new ExchangeCreateRequest(itemAId, itemBId, "Swap Logic Analyzer for Arduino Mega");
        MvcResult exRes = mockMvc.perform(post("/api/v1/exchanges")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activeProp)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        Long exId = objectMapper.readTree(exRes.getResponse().getContentAsString()).get("id").asLong();

        // Recipient (studentToken) accepts the proposal -> locks both items in ascending ID order
        mockMvc.perform(put("/api/v1/exchanges/" + exId + "/accept")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DecisionNoteRequest("Great swap, let's meet at Tech Park."))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Proposer confirms handover -> HANDOVER_PENDING
        mockMvc.perform(put("/api/v1/exchanges/" + exId + "/confirm-handover")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("HANDOVER_PENDING"));

        // Recipient confirms handover -> COMPLETED
        mockMvc.perform(put("/api/v1/exchanges/" + exId + "/confirm-handover")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(21)
    @DisplayName("TC33 (Sprint 2): Rental Quote Calculation Endpoint, Handover, Completion & Payment Integrity")
    void test21_sprint2RentalQuoteHandoverAndCompletion() throws Exception {
        String rStart = LocalDate.now().plusDays(50).toString();
        String rEnd = LocalDate.now().plusDays(55).toString(); // 5 days * ₹50 = ₹250 + ₹300 deposit = ₹550

        // 1. Test POST /api/v1/rentals/calculate
        RentalCalculateRequest calcReq = new RentalCalculateRequest(createdRentItemId, rStart, rEnd);
        mockMvc.perform(post("/api/v1/rentals/calculate")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(calcReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rentalDays").value(5))
            .andExpect(jsonPath("$.dailyRate").value(50.00))
            .andExpect(jsonPath("$.securityDeposit").value(500.00))
            .andExpect(jsonPath("$.totalRentalAmount").value(250.00))
            .andExpect(jsonPath("$.totalPayableWithDeposit").value(750.00))
            .andExpect(jsonPath("$.availableForDates").value(true));

        // 2. Test GET /api/v1/rentals/calculate
        mockMvc.perform(get("/api/v1/rentals/calculate")
                .param("itemId", createdRentItemId.toString())
                .param("startDate", rStart)
                .param("endDate", rEnd)
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rentalDays").value(5))
            .andExpect(jsonPath("$.totalRentalAmount").value(250.00));

        // 3. Create unpaid rental, approve, handover, and complete return (verifying paymentStatus stays PENDING)
        RentalCreateRequest rentReq = new RentalCreateRequest(createdRentItemId, rStart, rEnd, false);
        MvcResult rRes = mockMvc.perform(post("/api/v1/rentals")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rentReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
            .andReturn();
        Long rentId = objectMapper.readTree(rRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/rentals/" + rentId + "/approve")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        mockMvc.perform(put("/api/v1/rentals/" + rentId + "/handover")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/v1/rentals/" + rentId + "/complete")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
    }

    @Test
    @Order(22)
    @DisplayName("TC34 (Sprint 2): Multiple Donation Requests Atomic Rejection & Single-Recipient Enforcement")
    void test22_sprint2MultipleDonationRequestsAtomicRejection() throws Exception {
        // Create a new DONATE item owned by studentToken
        ItemUpsertRequest donateItem = new ItemUpsertRequest(
            "Engineering Graphics Drafter & Scale Set",
            "Complete mini-drafter in canvas bag for first-year students.",
            4L, "Drafting Tools", "GOOD", "DONATE", "AVAILABLE",
            "Main Campus Clock Tower", BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, 365, null, "Free for first-year students", null, List.of()
        );
        MvcResult itemRes = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(donateItem)))
            .andExpect(status().isCreated())
            .andReturn();
        Long multiDonateItemId = objectMapper.readTree(itemRes.getResponse().getContentAsString()).get("id").asLong();

        // Student 2 (Diya) submits Donation Request #1
        MvcResult req1Res = mockMvc.perform(post("/api/v1/donations")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DonationCreateRequest(multiDonateItemId, "Need drafter for EG lab."))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        Long req1Id = objectMapper.readTree(req1Res.getResponse().getContentAsString()).get("id").asLong();

        // Student 3 (Aarav) submits competing Donation Request #2 for the same item
        MvcResult loginAarav = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("aarav.krishnan@srmist.edu.in", "ReCircle@2026"))))
            .andExpect(status().isOk())
            .andReturn();
        String aaravToken = objectMapper.readTree(loginAarav.getResponse().getContentAsString()).get("accessToken").asText();

        MvcResult req2Res = mockMvc.perform(post("/api/v1/donations")
                .header("Authorization", "Bearer " + aaravToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DonationCreateRequest(multiDonateItemId, "Helping a junior in my hostel."))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        Long req2Id = objectMapper.readTree(req2Res.getResponse().getContentAsString()).get("id").asLong();

        // Donor approves Request #1 -> Request #2 MUST be atomically rejected!
        mockMvc.perform(put("/api/v1/donations/requests/" + req1Id + "/approve")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/donations/requests/" + req2Id)
                .header("Authorization", "Bearer " + aaravToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        // Complete handover of approved donation Request #1 -> COMPLETED
        mockMvc.perform(put("/api/v1/donations/requests/" + req1Id + "/complete")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(23)
    @DisplayName("TC35 (Sprint 2): Unified Transaction History & Individual Notification Read Tracking")
    void test23_sprint2TransactionHistoryAndNotifications() throws Exception {
        // 1. Unified Transaction History must contain BORROW, RENT, EXCHANGE, DONATE without duplicate transaction codes
        MvcResult txRes = mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andReturn();

        JsonNode txArray = objectMapper.readTree(txRes.getResponse().getContentAsString());
        Set<String> types = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (JsonNode tx : txArray) {
            types.add(tx.get("transactionType").asText());
            String code = tx.get("transactionCode").asText();
            assertThat(codes.add(code)).as("Duplicate transaction code check: " + code).isTrue();
        }
        assertThat(types).contains("BORROW", "RENT", "EXCHANGE", "DONATE");

        // 2. Individual Notification mark-as-read (PUT /api/v1/notifications/{id}/read)
        MvcResult notifRes = mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode notifRoot = objectMapper.readTree(notifRes.getResponse().getContentAsString());
        int unreadBefore = notifRoot.get("unreadCount").asInt();
        assertThat(unreadBefore).isGreaterThan(0);

        Long firstUnreadId = null;
        for (JsonNode n : notifRoot.get("notifications")) {
            if (!n.get("read").asBoolean()) {
                firstUnreadId = n.get("id").asLong();
                break;
            }
        }
        assertThat(firstUnreadId).isNotNull();

        mockMvc.perform(put("/api/v1/notifications/" + firstUnreadId + "/read")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk());

        MvcResult notifAfterRes = mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andReturn();
        int unreadAfter = objectMapper.readTree(notifAfterRes.getResponse().getContentAsString()).get("unreadCount").asInt();
        assertThat(unreadAfter).isEqualTo(unreadBefore - 1);
    }

    @Test
    @Order(24)
    @DisplayName("TC36 (Sprint 2): Multi-Threaded Concurrent Approval Protection for Overlapping Item Requests")
    void test24_sprint2ConcurrentRequestsForSameItem() throws Exception {
        // Create a dedicated BORROW item for concurrency test
        ItemUpsertRequest concItem = new ItemUpsertRequest(
            "Oscilloscope Probe Set (Concurrency Test)",
            "100MHz oscilloscope probes for concurrent booking test.",
            3L, "Measurement", "GOOD", "BORROW", "AVAILABLE",
            "Tech Park Lab", BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, 14, null, "Lab use only", null, List.of()
        );
        MvcResult itemRes = mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(concItem)))
            .andExpect(status().isCreated())
            .andReturn();
        Long concItemId = objectMapper.readTree(itemRes.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loginAarav = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("aarav.krishnan@srmist.edu.in", "ReCircle@2026"))))
            .andExpect(status().isOk())
            .andReturn();
        String aaravToken = objectMapper.readTree(loginAarav.getResponse().getContentAsString()).get("accessToken").asText();

        String cStart = LocalDate.now().plusDays(10).toString();
        String cEnd = LocalDate.now().plusDays(13).toString();

        // Two students submit overlapping PENDING borrow requests for the exact same dates
        MvcResult b1 = mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BorrowCreateRequest(concItemId, cStart, cEnd, "Concurrent request 1"))))
            .andExpect(status().isCreated())
            .andReturn();
        Long req1Id = objectMapper.readTree(b1.getResponse().getContentAsString()).get("id").asLong();

        MvcResult b2 = mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + aaravToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BorrowCreateRequest(concItemId, cStart, cEnd, "Concurrent request 2"))))
            .andExpect(status().isCreated())
            .andReturn();
        Long req2Id = objectMapper.readTree(b2.getResponse().getContentAsString()).get("id").asLong();

        // Fire two concurrent approval requests using ExecutorService + CountDownLatch
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch startGate = new CountDownLatch(1);

            Future<Integer> f1 = pool.submit(() -> {
                ready.countDown();
                startGate.await();
                return mockMvc.perform(put("/api/v1/borrow-requests/" + req1Id + "/approve")
                        .header("Authorization", "Bearer " + studentToken))
                    .andReturn().getResponse().getStatus();
            });

            Future<Integer> f2 = pool.submit(() -> {
                ready.countDown();
                startGate.await();
                return mockMvc.perform(put("/api/v1/borrow-requests/" + req2Id + "/approve")
                        .header("Authorization", "Bearer " + studentToken))
                    .andReturn().getResponse().getStatus();
            });

            ready.await();
            startGate.countDown();
            int status1 = f1.get();
            int status2 = f2.get();

            // Exactly ONE approval must succeed (200 OK); the other must fail (400/409) because the first auto-rejected or locked it!
            int successCount = (status1 == 200 ? 1 : 0) + (status2 == 200 ? 1 : 0);
            assertThat(successCount).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Order(25)
    @DisplayName("TC37 (Sprint 3): US16 Trust Score Neutral Baselines, 4-Pillar Calculation & Clamping")
    void test25_sprint3TrustScoreFormulasAndClamping() throws Exception {
        // Register a fresh brand-new user with unverified email
        RegisterRequest freshReq = new RegisterRequest(
            "Ananya Sen",
            "ananya.s@srmist.edu.in",
            "SecurePass@2026",
            "SecurePass@2026",
            "RA2211003010888",
            "Biomedical Engineering",
            2,
            "+91 98409 77665",
            "Kalpana Chawla Hostel",
            "Biomedical Sophomore.",
            "Bio Tech Lab",
            null
        );
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(freshReq)))
            .andExpect(status().isCreated())
            .andReturn();
        Long freshUserId = objectMapper.readTree(res.getResponse().getContentAsString()).get("user").get("id").asLong();

        // 1. Fresh unverified user: verifiedAccount=0 (weight 15% -> 0), completedTx=0 (weight 35% -> 0), review neutral 50.0 (weight 30% -> 15.0), onTime neutral 50.0 (weight 20% -> 10.0) -> Total = 25.00
        mockMvc.perform(get("/api/v1/users/" + freshUserId + "/trust-score"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verifiedAccountScore").value(0.00))
            .andExpect(jsonPath("$.completedTxScore").value(0.00))
            .andExpect(jsonPath("$.userReviewScore").value(50.00))
            .andExpect(jsonPath("$.onTimeReturnScore").value(50.00))
            .andExpect(jsonPath("$.totalScore").value(25.00));

        // 2. Now verify email: verifiedAccount=100 (weight 15% -> 15.0) -> Total = 15 + 0 + 15 + 10 = 40.00
        mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VerifyEmailRequest("ananya.s@srmist.edu.in", "123456"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/" + freshUserId + "/trust-score"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verifiedAccountScore").value(100.00))
            .andExpect(jsonPath("$.totalScore").value(40.00));
    }

    @Test
    @Order(26)
    @DisplayName("TC38 (Sprint 3): US17 Peer Reviews - Non-Participant Authorization, Star Validation & Reviews Feed")
    void test26_sprint3PeerReviewsAuthorizationAndFeed() throws Exception {
        // Login Aarav who was NOT part of completedBorrowTxId
        MvcResult loginAarav = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("aarav.krishnan@srmist.edu.in", "ReCircle@2026"))))
            .andExpect(status().isOk())
            .andReturn();
        String aaravToken = objectMapper.readTree(loginAarav.getResponse().getContentAsString()).get("accessToken").asText();

        // 1. Non-participant review attempt on completedBorrowTxId MUST return 403 FORBIDDEN
        ReviewCreateRequest unauthRev = new ReviewCreateRequest(completedBorrowTxId, 5, "I was not in this transaction!");
        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + aaravToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unauthRev)))
            .andExpect(status().isForbidden());

        // 2. Rating outside 1-5 stars MUST be rejected (400 BAD_REQUEST)
        ReviewCreateRequest invalidStarRev = new ReviewCreateRequest(completedBorrowTxId, 6, "Invalid 6 stars!");
        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidStarRev)))
            .andExpect(status().isBadRequest());

        // 3. User reviews endpoint (GET /api/v1/users/{id}/reviews) returns reviews received
        mockMvc.perform(get("/api/v1/users/" + registeredStudentId + "/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(27)
    @DisplayName("TC39 (Sprint 3): US18 & US19 Dispute Resolution, Re-Resolution Prevention & Role Security")
    void test27_sprint3DisputeReResolutionAndRoleSecurity() throws Exception {
        // 1. Student creates a dispute on completedBorrowTxId
        DisputeCreateRequest dispReq = new DisputeCreateRequest(
            completedBorrowTxId, createdBorrowItemId, registeredStudentId,
            "COMMUNICATION", "Dispute to test re-resolution", "Test description for repeated resolution.",
            "Evidence note", null
        );
        MvcResult dRes = mockMvc.perform(post("/api/v1/disputes")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dispReq)))
            .andExpect(status().isCreated())
            .andReturn();
        Long testDispId = objectMapper.readTree(dRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Regular student attempting to resolve dispute MUST be rejected with 403 FORBIDDEN
        mockMvc.perform(put("/api/v1/admin/disputes/" + testDispId + "/resolve")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DisputeResolveRequest("RESOLVED", "Student trying to resolve"))))
            .andExpect(status().isForbidden());

        // 3. Admin successfully resolves the dispute
        mockMvc.perform(put("/api/v1/admin/disputes/" + testDispId + "/resolve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DisputeResolveRequest("RESOLVED", "First resolution by admin", false))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"));

        // 4. Repeated resolution on already resolved dispute MUST be rejected (400 BAD_REQUEST / DISPUTE_ALREADY_RESOLVED)
        mockMvc.perform(put("/api/v1/admin/disputes/" + testDispId + "/resolve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DisputeResolveRequest("DISMISSED", "Attempting second resolution"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("DISPUTE_ALREADY_RESOLVED"));
    }

    @Test
    @Order(28)
    @DisplayName("TC40 (Sprint 3): US20 & US21 Admin Dashboard KPIs & Analytics Access Control")
    void test28_sprint3AdminDashboardAndAnalytics() throws Exception {
        // 1. Student access to /api/v1/admin/dashboard MUST be blocked (403 FORBIDDEN)
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        // 2. Admin access to /api/v1/admin/dashboard returns genuine KPIs
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRegisteredUsers").isNumber())
            .andExpect(jsonPath("$.verifiedUsers").isNumber())
            .andExpect(jsonPath("$.totalItemListings").isNumber())
            .andExpect(jsonPath("$.activeBorrowingTransactions").isNumber());

        // 3. Admin access to /api/v1/admin/analytics returns valid Recharts chart datasets
        mockMvc.perform(get("/api/v1/admin/analytics")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryDistribution").isArray())
            .andExpect(jsonPath("$.monthlyTransactions").isArray())
            .andExpect(jsonPath("$.trustDistribution").isArray());
    }

    @Test
    @Order(29)
    @DisplayName("TC41 (Sprint 3): US22 Admin User Management - Account Suspension & Reactivation")
    void test29_sprint3UserSuspensionAndReactivation() throws Exception {
        // 1. Student attempting to suspend another user MUST be rejected with 403 FORBIDDEN
        mockMvc.perform(put("/api/v1/admin/users/" + registeredStudentId + "/status")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED", "reason", "Student trying to suspend"))))
            .andExpect(status().isForbidden());

        // 2. Admin suspends the user
        mockMvc.perform(put("/api/v1/admin/users/" + registeredStudentId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED", "reason", "Administrative compliance review"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"));

        // 3. Admin reactivates the user
        mockMvc.perform(put("/api/v1/admin/users/" + registeredStudentId + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE", "reason", "Compliance review cleared"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    @Order(30)
    @DisplayName("TC42 (Sprint 3): US23 Immutable Audit History - Audit Logging & Filter Verification")
    void test30_sprint3AuditHistoryAndLogFilter() throws Exception {
        // 1. Student attempting to access audit logs MUST be blocked with 403 FORBIDDEN
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        // 2. Admin retrieves audit logs
        MvcResult auditRes = mockMvc.perform(get("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andReturn();

        JsonNode auditLogs = objectMapper.readTree(auditRes.getResponse().getContentAsString());
        assertThat(auditLogs.size()).isGreaterThanOrEqualTo(2);

        // Verify that audit log details do not contain sensitive tokens or passwords
        for (JsonNode entry : auditLogs) {
            assertThat(entry.has("actionType")).isTrue();
            assertThat(entry.has("actorName")).isTrue();
            if (entry.has("details") && !entry.get("details").isNull()) {
                String details = entry.get("details").asText();
                assertThat(details).doesNotContain("password");
                assertThat(details).doesNotContain("accessToken");
                assertThat(details).doesNotContain("token");
            }
        }
    }
}
