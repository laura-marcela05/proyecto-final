package com.corhuila.gymtracker.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.corhuila.gymtracker.dto.LoginRequest;
import com.corhuila.gymtracker.dto.LoginResponse;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.User;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.UserRepository;
import com.corhuila.gymtracker.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void login_withValidCredentials_returnsTokenAndProfile() {
        UUID id = UUID.randomUUID();
        User user = new User("trainer1", "Trainer One", "stored-hash", Role.TRAINER);
        ReflectionTestUtils.setField(user, "id", id);
        when(users.findByUsername("trainer1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test123*", "stored-hash")).thenReturn(true);
        when(jwtService.generateToken(id, Role.TRAINER)).thenReturn("issued-token");

        LoginResponse result = authService.login(new LoginRequest("trainer1", "Test123*"));

        assertThat(result).isEqualTo(new LoginResponse("issued-token", Role.TRAINER, "Trainer One"));
        verify(passwordEncoder).matches("Test123*", "stored-hash");
        verify(jwtService).generateToken(id, Role.TRAINER);
    }

    @Test
    void login_withUnknownUsername_returnsGenericErrorWithoutToken() {
        when(users.findByUsername("missing")).thenReturn(Optional.empty());

        assertInvalidCredentials(new LoginRequest("missing", "Test123*"));

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_withWrongPassword_returnsSameGenericErrorWithoutToken() {
        User user = new User("trainer1", "Trainer One", "stored-hash", Role.TRAINER);
        when(users.findByUsername("trainer1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertInvalidCredentials(new LoginRequest("trainer1", "wrong"));

        verifyNoInteractions(jwtService);
    }

    private void assertInvalidCredentials(LoginRequest request) {
        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
