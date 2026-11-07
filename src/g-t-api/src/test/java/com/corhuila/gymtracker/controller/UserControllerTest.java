package com.corhuila.gymtracker.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.security.AuthenticatedUser;
import com.corhuila.gymtracker.security.JwtService;
import com.corhuila.gymtracker.security.RestAccessDeniedHandler;
import com.corhuila.gymtracker.security.RestAuthenticationEntryPoint;
import com.corhuila.gymtracker.security.SecurityConfig;
import com.corhuila.gymtracker.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void listUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void listUsers_withTrainerToken_returns403() throws Exception {
        given(jwtService.parse("trainer-token"))
                .willReturn(Optional.of(new AuthenticatedUser(UUID.randomUUID(), Role.TRAINER)));

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer trainer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
