package com.corhuila.gymtracker.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.dto.UserCreateRequest;
import com.corhuila.gymtracker.dto.UserUpdateRequest;
import com.corhuila.gymtracker.dto.UserResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
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

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final String PATH = "/api/v1/users";
    private static final String CREATE_BODY = "{\"fullName\":\"Trainer One\",\"username\":\"trainer1\",\"password\":\"Test123*\",\"role\":\"TRAINER\"}";

    @ParameterizedTest
    @ValueSource(strings = {"POST", "GET", "PUT", "DELETE"})
    void userOperations_withoutToken_return401(String method) throws Exception {
        String path = "POST".equals(method) ? PATH : PATH + "/" + USER_ID;
        mockMvc.perform(request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY)).andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "GET", "PUT", "DELETE"})
    void userOperations_asTrainer_return403(String method) throws Exception {
        given(jwtService.parse("trainer-token"))
                .willReturn(Optional.of(new AuthenticatedUser(UUID.randomUUID(), Role.TRAINER)));
        String path = "POST".equals(method) ? PATH : PATH + "/" + USER_ID;
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer trainer-token")
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    void listUsers_asAdmin_returnsProfilesWithoutHashes() throws Exception {
        authenticateAdmin();
        given(userService.list()).willReturn(List.of(response()));
        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void createUser_asAdmin_returns201AndLocation() throws Exception {
        authenticateAdmin();
        given(userService.create(new UserCreateRequest("Trainer One", "trainer1", "Test123*", Role.TRAINER)))
                .willReturn(response());
        mockMvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Location", PATH + "/" + USER_ID))
                .andExpect(jsonPath("$.username").value("trainer1"));
    }

    @Test
    void createUser_withShortPassword_returns400() throws Exception {
        authenticateAdmin();
        mockMvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY.replace("Test123*", "short")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(userService);
    }

    @Test
    void getUser_asAdmin_returnsProfile() throws Exception {
        authenticateAdmin();
        given(userService.get(USER_ID)).willReturn(response());
        mockMvc.perform(get(PATH + "/" + USER_ID).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(USER_ID.toString()));
    }

    @Test
    void updateUser_asAdmin_passesNewDetailsToService() throws Exception {
        authenticateAdmin();
        UserUpdateRequest update = new UserUpdateRequest("Updated", "updated", null);
        given(userService.update(USER_ID, update))
                .willReturn(new UserResponse(USER_ID, "updated", "Updated", Role.TRAINER));
        mockMvc.perform(put(PATH + "/" + USER_ID).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Updated\",\"username\":\"updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated"));
        verify(userService).update(USER_ID, update);
    }

    @Test
    void deleteUser_asAdmin_passesCallerAndReturns204() throws Exception {
        authenticateAdmin();
        mockMvc.perform(delete(PATH + "/" + USER_ID).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(userService).delete(USER_ID, ADMIN_ID);
    }

    private void authenticateAdmin() {
        given(jwtService.parse("admin-token"))
                .willReturn(Optional.of(new AuthenticatedUser(ADMIN_ID, Role.ADMIN)));
    }

    private static UserResponse response() {
        return new UserResponse(USER_ID, "trainer1", "Trainer One", Role.TRAINER);
    }
}
