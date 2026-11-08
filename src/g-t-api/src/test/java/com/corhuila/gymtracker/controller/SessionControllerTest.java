package com.corhuila.gymtracker.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.corhuila.gymtracker.dto.SessionResponse;
import com.corhuila.gymtracker.entity.MuscleGroup;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.SessionStatus;
import com.corhuila.gymtracker.security.AuthenticatedUser;
import com.corhuila.gymtracker.security.JwtService;
import com.corhuila.gymtracker.security.RestAccessDeniedHandler;
import com.corhuila.gymtracker.security.RestAuthenticationEntryPoint;
import com.corhuila.gymtracker.security.SecurityConfig;
import com.corhuila.gymtracker.service.SessionService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessionController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SessionControllerTest {
    private static final UUID CALLER = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID STUDENT = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID SESSION = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final String HISTORY = "/api/v1/students/" + STUDENT + "/sessions";
    private static final String DETAIL = "/api/v1/sessions/" + SESSION;
    @Autowired private MockMvc mockMvc;
    @MockitoBean private SessionService sessionService;
    @MockitoBean private JwtService jwtService;

    @ParameterizedTest
    @ValueSource(strings = {"POST", "GET", "PUT", "PATCH", "DELETE"})
    void sessionRoutes_withoutToken_return401(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path(method))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"closed\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void sessionRoutes_withWrongRole_return403(String method) throws Exception {
        authenticate("DELETE".equals(method) ? Role.TRAINER : Role.ADMIN);
        mockMvc.perform(request(HttpMethod.valueOf(method), path(method))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"closed\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sessionService);
    }

    @Test
    void checkIn_asTrainer_returns201AndLocation() throws Exception {
        authenticate(Role.TRAINER);
        given(sessionService.checkIn(STUDENT, CALLER, Role.TRAINER)).willReturn(open());
        mockMvc.perform(post(HISTORY).header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", DETAIL))
                .andExpect(jsonPath("$.status").value("open"));
        verify(sessionService).checkIn(STUDENT, CALLER, Role.TRAINER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TRAINER"})
    void history_withAllowedRole_passesStatusFilter(String roleName) throws Exception {
        Role role = Role.valueOf(roleName);
        authenticate(role);
        given(sessionService.list(STUDENT, CALLER, role, "closed")).willReturn(List.of(closed()));
        mockMvc.perform(get(HISTORY).param("status", "closed")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("closed"))
                .andExpect(jsonPath("$[0].durationMinutes").value(60));
        verify(sessionService).list(STUDENT, CALLER, role, "closed");
    }

    @Test
    void logMuscleGroup_asTrainer_passesSelectedGroup() throws Exception {
        authenticate(Role.TRAINER);
        given(sessionService.logMuscleGroup(SESSION, "chest", CALLER, Role.TRAINER)).willReturn(open());
        mockMvc.perform(put(DETAIL + "/muscle-groups/chest")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(SESSION.toString()));
        verify(sessionService).logMuscleGroup(SESSION, "chest", CALLER, Role.TRAINER);
    }

    @Test
    void checkOut_asTrainer_returnsClosedSession() throws Exception {
        authenticate(Role.TRAINER);
        given(sessionService.checkOut(SESSION, CALLER, Role.TRAINER)).willReturn(closed());
        mockMvc.perform(patch(DETAIL).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"closed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("closed"));
        verify(sessionService).checkOut(SESSION, CALLER, Role.TRAINER);
    }

    @Test
    void checkOut_withUnsupportedStatus_returns400() throws Exception {
        authenticate(Role.TRAINER);
        mockMvc.perform(patch(DETAIL).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"open\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(sessionService);
    }

    @Test
    void delete_asAdmin_returns204() throws Exception {
        authenticate(Role.ADMIN);
        mockMvc.perform(delete(DETAIL).header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(sessionService).delete(SESSION, CALLER, Role.ADMIN);
    }

    private static String path(String method) {
        return switch (method) {
            case "POST", "GET" -> HISTORY;
            case "PUT" -> DETAIL + "/muscle-groups/chest";
            default -> DETAIL;
        };
    }

    private void authenticate(Role role) {
        given(jwtService.parse("test-token")).willReturn(Optional.of(new AuthenticatedUser(CALLER, role)));
    }

    private static SessionResponse open() {
        return new SessionResponse(SESSION, STUDENT, SessionStatus.open,
                Instant.parse("2026-01-01T10:00:00Z"), null, null, List.of());
    }

    private static SessionResponse closed() {
        return new SessionResponse(SESSION, STUDENT, SessionStatus.closed,
                Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"),
                60L, List.of(MuscleGroup.chest));
    }
}
