package com.dhl.account.integration;

import com.dhl.account.DhlAccountApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DhlAccountApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, String> validRegisterPayload;

    @BeforeEach
    public void setUp() {
        validRegisterPayload = new HashMap<>();
        validRegisterPayload.put("fullName", "Jane Smith");
        validRegisterPayload.put("email", "jane.smith@dhl-test.com");
        validRegisterPayload.put("mobileNumber", "+1234567890");
        validRegisterPayload.put("password", "SecureDHLPassword123!");
        validRegisterPayload.put("confirmPassword", "SecureDHLPassword123!");
    }

    @Test
    public void testRegister_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", containsString("Registration successful")))
                .andExpect(jsonPath("$.email", is("jane.smith@dhl-test.com")));
    }

    @Test
    public void testRegister_InvalidFields_ReturnsBadRequest() throws Exception {
        Map<String, String> invalidPayload = new HashMap<>(validRegisterPayload);
        invalidPayload.put("email", "not-a-valid-email");
        invalidPayload.put("password", "Short1!"); // violates only size constraint (7 chars), but meets complexity requirements

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email", is("Please provide a valid email address")))
                .andExpect(jsonPath("$.password", is("Password must be at least 8 characters long")));
    }

    @Test
    public void testVerify_InvalidCodeFormat_ReturnsBadRequest() throws Exception {
        Map<String, String> verifyPayload = new HashMap<>();
        verifyPayload.put("email", "jane.smith@dhl-test.com");
        verifyPayload.put("code", "123"); // Needs 6 digits

        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("Verification code must be exactly 6 digits")));
    }
}
