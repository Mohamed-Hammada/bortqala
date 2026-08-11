package com.bemo.hr.shared.shortcut.api;

import com.bemo.hr.shared.api.ApiExceptionHandler;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.shared.shortcut.application.UserScreenShortcutService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScreenShortcutControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserScreenShortcutService shortcutService;

    @Mock
    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ApiExceptionHandler exceptionHandler = new ApiExceptionHandler(translationService);
        mockMvc = MockMvcBuilders.standaloneSetup(new ScreenShortcutController(shortcutService))
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void authenticatedGetReturnsProfile() throws Exception {
        ScreenShortcutApi.ProfileResponse mockResponse = new ScreenShortcutApi.ProfileResponse(
                "DEFAULT", 0L, List.of(), List.of(), null
        );
        when(shortcutService.getProfile("admin")).thenReturn(mockResponse);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "N/A");

        mockMvc.perform(get("/api/v1/auth/preferences/shortcuts").principal(auth))
                .andExpect(status().isOk());

        verify(shortcutService).getProfile("admin");
    }

    @Test
    void putRejectsMalformedKeyCode() throws Exception {
        ScreenShortcutApi.ReplaceShortcutsRequest badReq = new ScreenShortcutApi.ReplaceShortcutsRequest(
                0L,
                List.of(new ScreenShortcutApi.ShortcutItemRequest("INVALID_KEY", "EMPLOYEES", true))
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "N/A");

        mockMvc.perform(put("/api/v1/auth/preferences/shortcuts")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postResetInvokesResetToDefaults() throws Exception {
        ScreenShortcutApi.ProfileResponse mockResponse = new ScreenShortcutApi.ProfileResponse(
                "DEFAULT", 0L, List.of(), List.of(), null
        );
        when(shortcutService.resetToDefaults("admin")).thenReturn(mockResponse);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "N/A");

        mockMvc.perform(post("/api/v1/auth/preferences/shortcuts/reset").principal(auth))
                .andExpect(status().isOk());

        verify(shortcutService).resetToDefaults("admin");
    }
}
