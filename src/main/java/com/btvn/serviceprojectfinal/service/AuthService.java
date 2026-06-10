package com.btvn.serviceprojectfinal.service;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.model.dto.request.LoginRequest;
import com.btvn.serviceprojectfinal.model.dto.request.RefreshTokenRequest;
import com.btvn.serviceprojectfinal.model.dto.request.RegisterRequest;
import com.btvn.serviceprojectfinal.model.dto.response.AuthResponse;
import com.btvn.serviceprojectfinal.model.entity.TokenBlacklist;
import com.btvn.serviceprojectfinal.model.entity.User;
import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import com.btvn.serviceprojectfinal.repository.TokenBlacklistRepository;
import com.btvn.serviceprojectfinal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Chỉ cho phép đăng ký EMPLOYER hoặc CANDIDATE
        if (request.getRole() == RoleEnum.ADMIN) {
            throw new AppException("Không thể đăng ký tài khoản Admin",
                    HttpStatus.FORBIDDEN);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã tồn tại trong hệ thống",
                    HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(
                user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }


    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (DisabledException e) {
            throw new AppException("Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        } catch (BadCredentialsException e) {
            throw new AppException("Email hoặc mật khẩu không chính xác",
                    HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("Người dùng không tồn tại",
                        HttpStatus.NOT_FOUND));

        String accessToken = jwtService.generateAccessToken(
                user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(
                user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }


    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AppException("Token không hợp lệ", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        if (tokenBlacklistRepository.existsByTokenString(token)) {
            throw new AppException("Token đã bị thu hồi trước đó",
                    HttpStatus.UNAUTHORIZED);
        }

        TokenBlacklist blacklisted = TokenBlacklist.builder()
                .tokenString(token)
                .revokedAt(LocalDateTime.now())
                .build();

        tokenBlacklistRepository.save(blacklisted);
    }


    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new AppException("Refresh token không hợp lệ hoặc đã hết hạn",
                    HttpStatus.UNAUTHORIZED);
        }

        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new AppException("Token không đúng loại, yêu cầu Refresh Token",
                    HttpStatus.UNAUTHORIZED);
        }

        String email = jwtService.extractEmail(refreshToken);
        String role = jwtService.extractRole(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại",
                        HttpStatus.NOT_FOUND));

        if (!user.getIsActive()) {
            throw new AppException("Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        }

        // Blacklist refresh token cũ sau khi dùng (Rotation)
        TokenBlacklist blacklisted = TokenBlacklist.builder()
                .tokenString(refreshToken)
                .revokedAt(LocalDateTime.now())
                .build();
        tokenBlacklistRepository.save(blacklisted);

        // Cấp token mới
        String newAccessToken = jwtService.generateAccessToken(email, role);
        String newRefreshToken = jwtService.generateRefreshToken(email, role);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .email(email)
                .role(role)
                .build();
    }
}