package com.btvn.serviceprojectfinal.controller.candidate;

import com.btvn.serviceprojectfinal.model.dto.request.ChangePasswordRequest;
import com.btvn.serviceprojectfinal.model.dto.request.candidate.ApplyJobRequest;
import com.btvn.serviceprojectfinal.model.dto.response.ApiResponse;
import com.btvn.serviceprojectfinal.model.dto.response.ApplicationResponse;
import com.btvn.serviceprojectfinal.model.dto.response.CvUploadResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.JobPostingResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.PageResponse;
import com.btvn.serviceprojectfinal.model.entity.enums.ApplicationStatusEnum;
import com.btvn.serviceprojectfinal.service.candidate.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    // Tìm kiếm việc làm (job APPROVED)
    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<PageResponse<JobPostingResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<JobPostingResponse> data =
                candidateService.searchJobs(keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Tìm kiếm việc làm thành công", data));
    }

    // Nộp hồ sơ ứng tuyển
    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyJob(
            @Valid @RequestBody ApplyJobRequest request) {

        ApplicationResponse data = candidateService.applyJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Nộp hồ sơ thành công", data));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationResponse>>> getMyApplications(
            @RequestParam(required = false) ApplicationStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ApplicationResponse> data =
                candidateService.getMyApplications(status, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách hồ sơ thành công", data));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable Long id) {

        ApplicationResponse data = candidateService.getMyApplicationById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy chi tiết hồ sơ thành công", data));
    }

    @PostMapping(value = "/cv/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CvUploadResponse>> uploadCv(
            @RequestParam("file") MultipartFile file) {

        CvUploadResponse data = candidateService.uploadCv(file);
        return ResponseEntity.ok(
                ApiResponse.success("Tải lên CV thành công", data));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        candidateService.changePassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}