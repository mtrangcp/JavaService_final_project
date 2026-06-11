package com.btvn.serviceprojectfinal.controller;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.exception.GlobalExceptionHandler;
import com.btvn.serviceprojectfinal.model.dto.request.LoginRequest;
import com.btvn.serviceprojectfinal.model.dto.request.RegisterRequest;
import com.btvn.serviceprojectfinal.model.dto.response.AuthResponse;
import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import com.btvn.serviceprojectfinal.repository.TokenBlacklistRepository;
import com.btvn.serviceprojectfinal.security.CustomUserDetailsService;
import com.btvn.serviceprojectfinal.service.AuthService;
import com.btvn.serviceprojectfinal.service.JwtService;
import com.btvn.serviceprojectfinal.service.candidate.CandidateService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean CandidateService candidateService;

    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    TokenBlacklistRepository tokenBlacklistRepository;

    private AuthResponse mockAuthResponse() {
        return AuthResponse.builder()
                .accessToken("mockAccessToken")
                .refreshToken("mockRefreshToken")
                .tokenType("Bearer")
                .email("test@gmail.com")
                .role("CANDIDATE")
                .build();
    }

    // ===== TEST 11 =====
    @Test
    @DisplayName("CT-01: POST /login thành công → 200 OK + accessToken")
    void login_ValidRequest_Return200WithToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(mockAuthResponse());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("mockAccessToken"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.role").value("CANDIDATE"));
    }

    // ===== TEST 12 =====
    @Test
    @DisplayName("CT-02: POST /login với email sai định dạng → 400 Bad Request")
    void login_InvalidEmail_Return400() throws Exception {
        // Arrange — email không đúng định dạng
        LoginRequest request = new LoginRequest();
        request.setEmail("not-an-email");
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.email").exists());
    }

    // ===== TEST 13 =====
    @Test
    @DisplayName("CT-03: POST /register thành công → 201 Created")
    void register_ValidRequest_Return201() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("test@gmail.com");
        request.setPassword("password123");
        request.setRole(RoleEnum.CANDIDATE);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(mockAuthResponse());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Đăng ký thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("mockAccessToken"));
    }

    // ===== TEST 14 =====
    @Test
    @DisplayName("CT-04: POST /register với email đã tồn tại → 409 Conflict")
    void register_DuplicateEmail_Return409() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("test@gmail.com");
        request.setPassword("password123");
        request.setRole(RoleEnum.CANDIDATE);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AppException(
                        "Email đã tồn tại trong hệ thống", HttpStatus.CONFLICT));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email đã tồn tại trong hệ thống"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ===== TEST 15 =====
    @Test
    @WithMockUser // Giả lập user đã đăng nhập
    @DisplayName("CT-05: POST /logout thành công → 200 OK")
    void logout_ValidToken_Return200() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer mockAccessToken")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));
    }
}