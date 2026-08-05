package com.bemo.hr.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "hr.security.demo-no-login.enabled=true",
        "hr.security.demo-no-login.secret=fixed-test-demo-secret",
        "hr.security.demo-no-login.app-code=TEST",
        "hr.security.demo-no-login.app-name=Bemo Automated Test",
        "hr.security.demo-no-login.profiles=",
})
class DemoNoLoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validSecretExchangesForSuperAdminSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"fixed-test-demo-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.app.code").value("TEST"))
                .andExpect(jsonPath("$.user.roles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.user.username").value("demo_superadmin"));
    }

    @Test
    void wrongSecretIsRejectedWithNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"wrong-secret\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEMO_NO_LOGIN_LINK_INVALID"));
    }

    @Test
    void missingSecretIsRejectedWithNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/auth/demo-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEMO_NO_LOGIN_LINK_INVALID"));
    }
}
