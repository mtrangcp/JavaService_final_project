package com.btvn.serviceprojectfinal.controller.employer;

import com.btvn.serviceprojectfinal.model.dto.request.employer.CreateJobRequest;
import com.btvn.serviceprojectfinal.model.dto.request.employer.UpdateApplicationStatusRequest;
import com.btvn.serviceprojectfinal.model.dto.request.employer.UpdateJobRequest;
import com.btvn.serviceprojectfinal.model.dto.response.ApiResponse;
import com.btvn.serviceprojectfinal.model.dto.response.ApplicationResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.JobPostingResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.PageResponse;
import com.btvn.serviceprojectfinal.model.entity.enums.ApplicationStatusEnum;
import com.btvn.serviceprojectfinal.service.employer.EmployerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final EmployerService employerService;

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<JobPostingResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request) {
        JobPostingResponse data = employerService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng tin tuyển dụng thành công", data));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponse>>> getMyJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<JobPostingResponse> data = employerService.getMyJobs(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin thành công", data));
    }

    @PutMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<JobPostingResponse>> updateJob(
            @PathVariable Long id,
            @RequestBody UpdateJobRequest request) {
        JobPostingResponse data = employerService.updateJob(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tin tuyển dụng thành công", data));
    }

    // Nộp tin lên duyệt
    @PatchMapping("/jobs/{id}/submit")
    public ResponseEntity<ApiResponse<JobPostingResponse>> submitForApproval(
            @PathVariable Long id) {
        JobPostingResponse data = employerService.submitForApproval(id);
        return ResponseEntity.ok(ApiResponse.success("Nộp tin lên duyệt thành công", data));
    }

    @PatchMapping("/jobs/{id}/close")
    public ResponseEntity<ApiResponse<JobPostingResponse>> closeJob(@PathVariable Long id) {
        JobPostingResponse data = employerService.closeJob(id);
        return ResponseEntity.ok(ApiResponse.success("Đóng tin tuyển dụng thành công", data));
    }

    // Xóa tin (DRAFT)
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        employerService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tin tuyển dụng thành công", null));
    }

    // Xem tất cả hồ sơ vào các job của mình
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) ApplicationStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ApplicationResponse> data =
                employerService.getApplicationsForMyJobs(status, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách hồ sơ thành công", data));
    }

    // Xem hồ sơ theo từng Job
    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationResponse>>> getApplicationsByJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) ApplicationStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ApplicationResponse> data =
                employerService.getApplicationsByJobId(jobId, status, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách hồ sơ theo job thành công", data));
    }

    // Cập nhật trạng thái hồ sơ
    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {

        ApplicationResponse data = employerService.updateApplicationStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật trạng thái hồ sơ thành công", data));
    }
}
