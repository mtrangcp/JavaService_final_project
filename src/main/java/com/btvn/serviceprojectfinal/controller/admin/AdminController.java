package com.btvn.serviceprojectfinal.controller.admin;

import com.btvn.serviceprojectfinal.model.dto.request.admin.UpdateJobStatusRequest;
import com.btvn.serviceprojectfinal.model.dto.request.admin.UpdateUserRequest;
import com.btvn.serviceprojectfinal.model.dto.response.ApiResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.JobPostingResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.PageResponse;
import com.btvn.serviceprojectfinal.model.dto.response.UserResponse;
import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import com.btvn.serviceprojectfinal.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // user
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleEnum role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<UserResponse> data = adminService.getAllUsers(keyword, role, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", data));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse data = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", data));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse data = adminService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", data));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long id) {
        adminService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("Vô hiệu hóa người dùng thành công", null));
    }

    // job
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponse>>> getAllJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<JobPostingResponse> data = adminService.getAllJobs(keyword, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin tuyển dụng thành công", data));
    }

    @PutMapping("/jobs/{id}/status")
    public ResponseEntity<ApiResponse<JobPostingResponse>> updateJobStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobStatusRequest request) {

        JobPostingResponse data = adminService.updateJobStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tin tuyển dụng thành công", data));
    }
}