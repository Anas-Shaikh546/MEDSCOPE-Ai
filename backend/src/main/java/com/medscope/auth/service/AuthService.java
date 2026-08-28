package com.medscope.auth.service;

import com.medscope.auth.dto.AuthResponse;
import com.medscope.auth.dto.LoginRequest;
import com.medscope.auth.dto.RegisterRequest;
import com.medscope.common.exception.ConflictException;
import com.medscope.common.exception.UnauthorizedException;
import com.medscope.security.JwtService;
import com.medscope.user.entity.User;
import com.medscope.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        // Normalize email consistently: trim, lowercase, then validate/check.
        // "ANAS@EMAIL.COM" and "anas@email.com" must be treated as the same user.
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .build();

        // Database also enforces uniqueness via a unique index (V1__create_users.sql) -
        // we never rely on the application-level check alone.
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return AuthResponse.bearer(token);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
