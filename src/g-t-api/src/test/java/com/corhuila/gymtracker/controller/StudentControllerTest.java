package com.corhuila.gymtracker.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.corhuila.gymtracker.dto.StudentRequest;
import com.corhuila.gymtracker.dto.StudentResponse;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.security.AuthenticatedUser;
import com.corhuila.gymtracker.security.JwtService;
import com.corhuila.gymtracker.security.RestAccessDeniedHandler;
import com.corhuila.gymtracker.security.RestAuthenticationEntryPoint;
import com.corhuila.gymtracker.security.SecurityConfig;
import com.corhuila.gymtracker.service.StudentService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class StudentControllerTest {
    private static final String PATH = "/api/v1/students";
    private static final String BODY = "{\"name\":\"Ana Torres\",\"document\":\"1082345678\"}";
    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private StudentService studentService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void listStudents_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
        verifyNoInteractions(studentService);
    }

    @Test
    void listStudents_asTrainer_returnsStudents() throws Exception {
        authenticate(Role.TRAINER);
        given(studentService.list(CALLER_ID, Role.TRAINER)).willReturn(List.of(response()));
        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(STUDENT_ID.toString()));
        verify(studentService).list(CALLER_ID, Role.TRAINER);
    }

    @Test
    void listStudents_asAdmin_returnsStudents() throws Exception {
        authenticate(Role.ADMIN);
        given(studentService.list(CALLER_ID, Role.ADMIN)).willReturn(List.of(response()));
        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana Torres"));
        verify(studentService).list(CALLER_ID, Role.ADMIN);
    }

    @Test
    void createStudent_asAdmin_returns403() throws Exception {
        authenticate(Role.ADMIN);
        mockMvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(studentService);
    }

    @Test
    void createStudent_asTrainer_returns201AndLocation() throws Exception {
        authenticate(Role.TRAINER);
        given(studentService.create(CALLER_ID, request())).willReturn(response());
        mockMvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Location", PATH + "/" + STUDENT_ID))
                .andExpect(jsonPath("$.trainerId").value(CALLER_ID.toString()))
                .andExpect(jsonPath("$.document").value("1082345678"));
        verify(studentService).create(CALLER_ID, request());
    }

    @Test
    void createStudent_withBlankName_returns400() throws Exception {
        authenticate(Role.TRAINER);
        mockMvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \",\"document\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(studentService);
    }

    @Test
    void createStudent_withDuplicateDocument_returns409() throws Exception {
        authenticate(Role.TRAINER);
        given(studentService.create(CALLER_ID, request()))
                .willThrow(new ApiException(ErrorCode.DOCUMENT_ALREADY_REGISTERED));
        mockMvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DOCUMENT_ALREADY_REGISTERED"));
    }

    @Test
    void getStudent_whenHiddenByService_returns404() throws Exception {
        authenticate(Role.TRAINER);
        given(studentService.get(STUDENT_ID, CALLER_ID, Role.TRAINER))
                .willThrow(new ApiException(ErrorCode.STUDENT_NOT_FOUND));
        mockMvc.perform(get(PATH + "/" + STUDENT_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void updateStudent_asTrainer_returnsUpdatedStudent() throws Exception {
        authenticate(Role.TRAINER);
        given(studentService.update(STUDENT_ID, CALLER_ID, Role.TRAINER, request())).willReturn(response());
        mockMvc.perform(put(PATH + "/" + STUDENT_ID).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(STUDENT_ID.toString()));
        verify(studentService).update(STUDENT_ID, CALLER_ID, Role.TRAINER, request());
    }

    @Test
    void updateStudent_asAdmin_returns403() throws Exception {
        authenticate(Role.ADMIN);
        mockMvc.perform(put(PATH + "/" + STUDENT_ID).header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(studentService);
    }

    @Test
    void deleteStudent_asTrainer_returns403() throws Exception {
        authenticate(Role.TRAINER);
        mockMvc.perform(delete(PATH + "/" + STUDENT_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(studentService);
    }

    @Test
    void deleteStudent_asAdmin_returns204() throws Exception {
        authenticate(Role.ADMIN);
        mockMvc.perform(delete(PATH + "/" + STUDENT_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(studentService).delete(STUDENT_ID, CALLER_ID, Role.ADMIN);
    }

    @Test
    void deleteStudent_withSessions_returns409() throws Exception {
        authenticate(Role.ADMIN);
        org.mockito.Mockito.doThrow(new ApiException(ErrorCode.STUDENT_HAS_SESSIONS))
                .when(studentService).delete(STUDENT_ID, CALLER_ID, Role.ADMIN);
        mockMvc.perform(delete(PATH + "/" + STUDENT_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("STUDENT_HAS_SESSIONS"));
    }

    private void authenticate(Role role) {
        given(jwtService.parse("test-token")).willReturn(Optional.of(new AuthenticatedUser(CALLER_ID, role)));
    }

    private static StudentRequest request() {
        return new StudentRequest("Ana Torres", "1082345678");
    }

    private static StudentResponse response() {
        return new StudentResponse(STUDENT_ID, "Ana Torres", "1082345678", CALLER_ID, "Trainer One");
    }
}
