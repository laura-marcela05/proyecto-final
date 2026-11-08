package com.corhuila.gymtracker.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.corhuila.gymtracker.dto.LoginRequest;
import com.corhuila.gymtracker.dto.LoginResponse;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.security.JwtService;
import com.corhuila.gymtracker.security.RestAccessDeniedHandler;
import com.corhuila.gymtracker.security.RestAuthenticationEntryPoint;
import com.corhuila.gymtracker.security.SecurityConfig;
import com.corhuila.gymtracker.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;

    @Test
    void login_withoutBearerToken_returnsProfileWithoutSecrets() throws Exception {
        given(authService.login(new LoginRequest("trainer1", "Test123*")))
                .willReturn(new LoginResponse("issued-token", Role.TRAINER, "Trainer One"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"trainer1\",\"password\":\"Test123*\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("issued-token"))
                .andExpect(jsonPath("$.role").value("TRAINER"))
                .andExpect(jsonPath("$.fullName").value("Trainer One"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        given(authService.login(new LoginRequest("trainer1", "wrong")))
                .willThrow(new ApiException(ErrorCode.INVALID_CREDENTIALS));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"trainer1\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withBlankUsername_returns400BeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"   \",\"password\":\"Test123*\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(authService);
    }

    @Test
    void login_withEmptyPassword_returns400BeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"trainer1\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(authService);
    }
}
