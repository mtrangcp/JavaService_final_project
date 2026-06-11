package com.btvn.serviceprojectfinal.service;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.model.dto.request.LoginRequest;
import com.btvn.serviceprojectfinal.model.dto.request.RegisterRequest;
import com.btvn.serviceprojectfinal.model.dto.response.AuthResponse;
import com.btvn.serviceprojectfinal.model.entity.User;
import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import com.btvn.serviceprojectfinal.repository.TokenBlacklistRepository;
import com.btvn.serviceprojectfinal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("test@gmail.com")
                .passwordHash("$2a$12$hashedPassword")
                .role(RoleEnum.CANDIDATE)
                .isActive(true)
                .build();
    }

    // ===== TEST 1 =====
    @Test
    @DisplayName("ST-01: Đăng ký thành công với role CANDIDATE")
    void register_ValidRequest_ReturnAuthResponse() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("test@gmail.com");
        request.setPassword("password123");
        request.setRole(RoleEnum.CANDIDATE);

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateAccessToken(anyString(), anyString()))
                .thenReturn("mockAccessToken");
        when(jwtService.generateRefreshToken(anyString(), anyString()))
                .thenReturn("mockRefreshToken");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("CANDIDATE", response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ===== TEST 2 =====
    @Test
    @DisplayName("ST-02: Đăng ký thất bại khi email đã tồn tại → 409 Conflict")
    void register_DuplicateEmail_ThrowAppException409() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");
        request.setRole(RoleEnum.CANDIDATE);
        request.setFullName("Test");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
        assertEquals("Email đã tồn tại trong hệ thống", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== TEST 3 =====
    @Test
    @DisplayName("ST-03: Đăng ký thất bại khi role là ADMIN → 403 Forbidden")
    void register_AdminRole_ThrowAppException403() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@gmail.com");
        request.setPassword("password123");
        request.setRole(RoleEnum.ADMIN);
        request.setFullName("Admin Test");

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals("Không thể đăng ký tài khoản Admin", exception.getMessage());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    // ===== TEST 4 =====
    @Test
    @DisplayName("ST-04: Đăng nhập thành công → trả về AccessToken và RefreshToken")
    void login_ValidCredentials_ReturnAuthResponse() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Không throw → xác thực thành công
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(mockUser));
        when(jwtService.generateAccessToken("test@gmail.com", "CANDIDATE"))
                .thenReturn("mockAccessToken");
        when(jwtService.generateRefreshToken("test@gmail.com", "CANDIDATE"))
                .thenReturn("mockRefreshToken");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("test@gmail.com", response.getEmail());
        assertEquals("CANDIDATE", response.getRole());
    }

    // ===== TEST 5 =====
    @Test
    @DisplayName("ST-05: Đăng nhập thất bại khi sai mật khẩu → 401 Unauthorized")
    void login_WrongPassword_ThrowAppException401() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
                () -> authService.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
        assertEquals("Email hoặc mật khẩu không chính xác", exception.getMessage());
    }
}