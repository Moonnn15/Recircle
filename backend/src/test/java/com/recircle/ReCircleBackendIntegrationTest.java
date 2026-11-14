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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:recircle_test_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
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

        // TC14 & TC29: Concurrent/Overlapping reservation attempt during reserved dates must be rejected
        mockMvc.perform(post("/api/v1/borrow-requests")
                .header("Authorization", "Bearer " + secondStudentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReq)))
            .andExpect(status().isBadRequest());

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
        // Seeded exchange offer #1 exists between Diya (u2) and Aarav (u1)
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

        mockMvc.perform(put("/api/v1/exchanges/1/complete")
                .header("Authorization", "Bearer " + aaravToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
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
}
