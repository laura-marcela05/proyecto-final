package com.corhuila.gymtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.corhuila.gymtracker.dto.UserCreateRequest;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.User;
import com.corhuila.gymtracker.dto.UserResponse;
import com.corhuila.gymtracker.dto.UserUpdateRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;
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
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createUser_encodesPasswordAndPreservesRole() {
        when(passwordEncoder.encode("Test123*")).thenReturn("encoded-hash");
        org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });

        UserResponse result = userService.create(new UserCreateRequest("Trainer One", "trainer1", "Test123*", Role.TRAINER));

        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(result.id()).isNotNull();
        assertThat(result.username()).isEqualTo("trainer1");
        assertThat(result.role()).isEqualTo(Role.TRAINER);
    }

    @Test
    void listUsers_mapsProfilesInRepositoryOrder() {
        User first = user(UUID.randomUUID());
        User second = user(UUID.randomUUID());
        when(users.findAllByOrderByFullNameAscIdAsc()).thenReturn(List.of(first, second));

        assertThat(userService.list()).extracting(UserResponse::id).containsExactly(first.getId(), second.getId());
    }

    @Test
    void getUser_whenMissing_returnsNotFound() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());

        assertError(() -> userService.get(id), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateUser_withoutPassword_keepsHashAndRole() {
        UUID id = UUID.randomUUID();
        User existing = user(id);
        when(users.findById(id)).thenReturn(Optional.of(existing));

        UserResponse result = userService.update(id, new UserUpdateRequest("Updated", "updated", null));

        assertThat(result.fullName()).isEqualTo("Updated");
        assertThat(result.username()).isEqualTo("updated");
        assertThat(existing.getPasswordHash()).isEqualTo("stored-hash");
        assertThat(result.role()).isEqualTo(Role.TRAINER);
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_withNewPassword_encodesIt() {
        UUID id = UUID.randomUUID();
        User existing = user(id);
        when(users.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("Changed123*")).thenReturn("new-hash");

        userService.update(id, new UserUpdateRequest("Updated", "updated", "Changed123*"));

        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
        assertThat(existing.getRole()).isEqualTo(Role.TRAINER);
    }

    @Test
    void updateUser_withDuplicateUsername_doesNotModifyUser() {
        UUID id = UUID.randomUUID();
        User existing = user(id);
        when(users.findById(id)).thenReturn(Optional.of(existing));
        when(users.existsByUsernameAndIdNot("taken", id)).thenReturn(true);

        assertError(() -> userService.update(id, new UserUpdateRequest("Changed", "taken", "Changed123*")),
                ErrorCode.USERNAME_ALREADY_TAKEN);
        assertThat(existing.getUsername()).isEqualTo("trainer1");
        assertThat(existing.getFullName()).isEqualTo("Trainer One");
        assertThat(existing.getPasswordHash()).isEqualTo("stored-hash");
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    @Test
    void deleteUser_whenDeletingOwnAccount_isRejected() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(user(id)));

        assertError(() -> userService.delete(id, id), ErrorCode.CANNOT_DELETE_OWN_USER);
        verify(users, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_withAssignedStudents_isRejected() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(user(id)));
        when(students.existsByTrainerId(id)).thenReturn(true);

        assertError(() -> userService.delete(id, UUID.randomUUID()), ErrorCode.USER_HAS_STUDENTS);
        verify(users, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_withoutStudents_deletesAccount() {
        UUID id = UUID.randomUUID();
        User existing = user(id);
        when(users.findById(id)).thenReturn(Optional.of(existing));

        userService.delete(id, UUID.randomUUID());

        verify(users).delete(existing);
    }


    @Test
    void updateUser_whenMissing_doesNotCheckDuplicatesOrEncodePassword() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());

        assertError(() -> userService.update(id,
                new UserUpdateRequest("Changed", "taken", "Changed123*")), ErrorCode.USER_NOT_FOUND);

        verify(users, never()).existsByUsernameAndIdNot(any(), any());
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUser_withOwnUsername_usesQueryExcludingOwnId() {
        UUID id = UUID.randomUUID();
        User existing = user(id);
        when(users.findById(id)).thenReturn(Optional.of(existing));

        UserResponse result = userService.update(id,
                new UserUpdateRequest("Updated", "trainer1", null));

        assertThat(result.username()).isEqualTo("trainer1");
        verify(users).existsByUsernameAndIdNot("trainer1", id);
        verify(users, never()).existsByUsername(any());
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    private static User user(UUID id) {
        User user = new User("trainer1", "Trainer One", "stored-hash", Role.TRAINER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ErrorCode code) {
        assertThatThrownBy(action).isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(code));
    }
}
