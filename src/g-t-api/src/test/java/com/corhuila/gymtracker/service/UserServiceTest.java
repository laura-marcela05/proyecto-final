package com.corhuila.gymtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.corhuila.gymtracker.dto.UserCreateRequest;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.StudentRepository;
import com.corhuila.gymtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private StudentRepository students;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_whenUsernameTaken_isRejected() {
        when(users.existsByUsername("trainer1")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(
                new UserCreateRequest("Laura Gómez", "trainer1", "Trainer123*", Role.TRAINER)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.USERNAME_ALREADY_TAKEN));
        verify(users, never()).save(any());
    }
}
