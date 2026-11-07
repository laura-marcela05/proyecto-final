package com.corhuila.gymtracker.service;

import com.corhuila.gymtracker.dto.LoginRequest;
import com.corhuila.gymtracker.dto.LoginResponse;
import com.corhuila.gymtracker.entity.User;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.UserRepository;
import com.corhuila.gymtracker.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** The same error for an unknown user and a wrong password, so the response never says which one failed. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        return new LoginResponse(jwtService.generateToken(user.getId(), user.getRole()), user.getRole(),
                user.getFullName());
    }
}
