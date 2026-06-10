package com.btvn.serviceprojectfinal.controller;

import com.btvn.serviceprojectfinal.model.dto.request.*;
import com.btvn.serviceprojectfinal.model.dto.response.ApiResponse;
import com.btvn.serviceprojectfinal.model.dto.response.AuthResponse;
import com.btvn.serviceprojectfinal.service.AuthService;
import com.btvn.serviceprojectfinal.service.candidate.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CandidateService candidateService;

    // FR-04: Đăng ký
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký thành công", data));
    }

    // FR-01: Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    // FR-03: Đăng xuất
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    // FR-02: Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse data = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Cấp lại token thành công", data));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        candidateService.forgotPassword(request);
        // Luôn trả về thông báo chung để không lộ email có tồn tại hay không
        return ResponseEntity.ok(ApiResponse.success(
                "Nếu email tồn tại trong hệ thống, mã OTP sẽ được gửi đến hộp thư của bạn",
                null));
    }

    // FR-10: Bước 2 — Xác nhận OTP & đặt lại mật khẩu
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        candidateService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }
}