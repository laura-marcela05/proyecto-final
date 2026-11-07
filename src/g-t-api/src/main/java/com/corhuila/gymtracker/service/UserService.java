package com.corhuila.gymtracker.service;

import com.corhuila.gymtracker.dto.UserCreateRequest;
import com.corhuila.gymtracker.dto.UserResponse;
import com.corhuila.gymtracker.dto.UserUpdateRequest;
import com.corhuila.gymtracker.entity.User;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.StudentRepository;
import com.corhuila.gymtracker.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, StudentRepository students, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.students = students;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAllByOrderByFullNameAscIdAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return UserResponse.from(find(userId));
    }

    /** BR-08: usernames are unique. BR-03: the role is chosen here and never changes. */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ApiException(ErrorCode.USERNAME_ALREADY_TAKEN);
        }
        User user = new User(request.username(), request.fullName(),
                passwordEncoder.encode(request.password()), request.role());
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public UserResponse update(UUID userId, UserUpdateRequest request) {
        User user = find(userId);
        if (users.existsByUsernameAndIdNot(request.username(), userId)) {
            throw new ApiException(ErrorCode.USERNAME_ALREADY_TAKEN);
        }
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        if (request.hasPassword()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(user);
    }

    /** BR-09: an admin cannot delete their own user. BR-11: a trainer who still owns students stays. */
    @Transactional
    public void delete(UUID userId, UUID callerId) {
        User user = find(userId);
        if (userId.equals(callerId)) {
            throw new ApiException(ErrorCode.CANNOT_DELETE_OWN_USER);
        }
        if (students.existsByTrainerId(userId)) {
            throw new ApiException(ErrorCode.USER_HAS_STUDENTS);
        }
        users.delete(user);
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
