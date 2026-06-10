package com.btvn.serviceprojectfinal.service.employer;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.model.dto.request.employer.CreateJobRequest;
import com.btvn.serviceprojectfinal.model.dto.request.employer.UpdateApplicationStatusRequest;
import com.btvn.serviceprojectfinal.model.dto.request.employer.UpdateJobRequest;
import com.btvn.serviceprojectfinal.model.dto.response.ApplicationResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.JobPostingResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.PageResponse;
import com.btvn.serviceprojectfinal.model.entity.Application;
import com.btvn.serviceprojectfinal.model.entity.JobPosting;
import com.btvn.serviceprojectfinal.model.entity.User;
import com.btvn.serviceprojectfinal.model.entity.enums.ApplicationStatusEnum;
import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import com.btvn.serviceprojectfinal.repository.ApplicationRepository;
import com.btvn.serviceprojectfinal.repository.JobPostingRepository;
import com.btvn.serviceprojectfinal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployerService {

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    private User getCurrentEmployer() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));
    }

    // FR-06: Đăng tin tuyển dụng mới
    @Transactional
    public JobPostingResponse createJob(CreateJobRequest request) {
        User employer = getCurrentEmployer();

        JobPosting job = JobPosting.builder()
                .employer(employer)
                .title(request.getTitle())
                .description(request.getDescription())
                .salaryRange(request.getSalaryRange())
                .status(JobStatusEnum.PENDING_APPROVAL)
                .build();

        jobPostingRepository.save(job);
        return mapToJobResponse(job);
    }

    // Lấy ds tin của employer
    public PageResponse<JobPostingResponse> getMyJobs(int page, int size) {
        User employer = getCurrentEmployer();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<JobPosting> jobPage = jobPostingRepository
                .findByEmployerId(employer.getId(), pageable);

        List<JobPostingResponse> content = jobPage.getContent()
                .stream()
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());

        return PageResponse.<JobPostingResponse>builder()
                .content(content)
                .pageNumber(jobPage.getNumber())
                .pageSize(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .last(jobPage.isLast())
                .build();
    }

    // Cập nhật tin tuyển dụng (chỉ khi DRAFT hoặc REJECTED)
    @Transactional
    public JobPostingResponse updateJob(Long jobId, UpdateJobRequest request) {
        User employer = getCurrentEmployer();
        JobPosting job = getJobOwnedByEmployer(jobId, employer.getId());

        // Chỉ sửa khi DRAFT hoặc REJECTED
        if (job.getStatus() == JobStatusEnum.APPROVED
                || job.getStatus() == JobStatusEnum.PENDING_APPROVAL) {
            throw new AppException(
                    "Không thể sửa tin đang ở trạng thái " + job.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            job.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            job.setDescription(request.getDescription());
        }
        if (request.getSalaryRange() != null) {
            job.setSalaryRange(request.getSalaryRange());
        }

        jobPostingRepository.save(job);
        return mapToJobResponse(job);
    }

    // Đóng tin tuyển dụng
    @Transactional
    public JobPostingResponse closeJob(Long jobId) {
        User employer = getCurrentEmployer();
        JobPosting job = getJobOwnedByEmployer(jobId, employer.getId());

        if (job.getStatus() == JobStatusEnum.CLOSED) {
            throw new AppException("Tin tuyển dụng đã đóng trước đó",
                    HttpStatus.BAD_REQUEST);
        }

        job.setStatus(JobStatusEnum.CLOSED);
        jobPostingRepository.save(job);
        return mapToJobResponse(job);
    }

    // Xóa tin (chỉ DRAFT mới được xóa cứng)
    @Transactional
    public void deleteJob(Long jobId) {
        User employer = getCurrentEmployer();
        JobPosting job = getJobOwnedByEmployer(jobId, employer.getId());

        if (job.getStatus() != JobStatusEnum.DRAFT) {
            throw new AppException(
                    "Chỉ có thể xóa tin ở trạng thái DRAFT",
                    HttpStatus.BAD_REQUEST);
        }

        jobPostingRepository.delete(job);
    }

    // Nộp tin lên Admin duyệt (DRAFT → PENDING_APPROVAL)
    @Transactional
    public JobPostingResponse submitForApproval(Long jobId) {
        User employer = getCurrentEmployer();
        JobPosting job = getJobOwnedByEmployer(jobId, employer.getId());

        if (job.getStatus() != JobStatusEnum.DRAFT
                && job.getStatus() != JobStatusEnum.REJECTED) {
            throw new AppException(
                    "Chỉ có thể nộp duyệt tin ở trạng thái DRAFT hoặc REJECTED",
                    HttpStatus.BAD_REQUEST);
        }

        job.setStatus(JobStatusEnum.PENDING_APPROVAL);
        jobPostingRepository.save(job);
        return mapToJobResponse(job);
    }


    // FR-08: Xem tất cả hồ sơ ứng tuyển vào các job của mình
    public PageResponse<ApplicationResponse> getApplicationsForMyJobs(
            ApplicationStatusEnum status, int page, int size) {
        User employer = getCurrentEmployer();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("appliedAt").descending());

        Page<Application> appPage = applicationRepository
                .findByEmployerId(employer.getId(), status, pageable);

        List<ApplicationResponse> content = appPage.getContent()
                .stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());

        return PageResponse.<ApplicationResponse>builder()
                .content(content)
                .pageNumber(appPage.getNumber())
                .pageSize(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    // FR-08: Xem hồ sơ theo từng Job cụ thể
    public PageResponse<ApplicationResponse> getApplicationsByJobId(
            Long jobId, ApplicationStatusEnum status, int page, int size) {
        User employer = getCurrentEmployer();

        // Kiểm tra job thuộc về employer này
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy tin tuyển dụng với ID: " + jobId,
                        HttpStatus.NOT_FOUND));

        if (!job.getEmployer().getId().equals(employer.getId())) {
            throw new AppException(
                    "Bạn không có quyền xem hồ sơ của tin này",
                    HttpStatus.FORBIDDEN);
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("appliedAt").descending());
        Page<Application> appPage = applicationRepository
                .findByJobPostingId(jobId, status, pageable);

        List<ApplicationResponse> content = appPage.getContent()
                .stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());

        return PageResponse.<ApplicationResponse>builder()
                .content(content)
                .pageNumber(appPage.getNumber())
                .pageSize(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    // FR-08: Cập nhật trạng thái hồ sơ
// AOP @AfterReturning sẽ tự động ghi log sau method này
    @Transactional
    public ApplicationResponse updateApplicationStatus(
            Long applicationId, UpdateApplicationStatusRequest request) {
        User employer = getCurrentEmployer();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy hồ sơ với ID: " + applicationId,
                        HttpStatus.NOT_FOUND));

        // Kiểm tra hồ sơ thuộc job của employer này
        if (!application.getJobPosting().getEmployer().getId().equals(employer.getId())) {
            throw new AppException(
                    "Bạn không có quyền cập nhật hồ sơ này",
                    HttpStatus.FORBIDDEN);
        }

        // Kiểm tra luồng trạng thái hợp lệ theo State Machine
        validateStatusTransition(application.getStatus(), request.getStatus());

        application.setStatus(request.getStatus());
        if (request.getFeedback() != null && !request.getFeedback().isBlank()) {
            application.setEmployerFeedback(request.getFeedback());
        }

        applicationRepository.save(application);
        return mapToApplicationResponse(application);
    }

    // Validate State Machine: UC-04 diagram
// PENDING → REVIEWING → INTERVIEWING → ACCEPTED / REJECTED
// PENDING → REJECTED (từ chối sớm)
// REVIEWING → REJECTED
    private void validateStatusTransition(ApplicationStatusEnum current,
                                          ApplicationStatusEnum next) {
        boolean valid = switch (current) {
            case PENDING -> next == ApplicationStatusEnum.REVIEWING
                    || next == ApplicationStatusEnum.REJECTED;
            case REVIEWING -> next == ApplicationStatusEnum.INTERVIEWING
                    || next == ApplicationStatusEnum.REJECTED;
            case INTERVIEWING -> next == ApplicationStatusEnum.ACCEPTED
                    || next == ApplicationStatusEnum.REJECTED;
            // ACCEPTED và REJECTED là trạng thái cuối
            case ACCEPTED, REJECTED -> false;
        };

        if (!valid) {
            throw new AppException(
                    String.format("Không thể chuyển trạng thái từ %s sang %s",
                            current, next),
                    HttpStatus.BAD_REQUEST);
        }
    }

    // Mapper (thêm vào EmployerService)
    private ApplicationResponse mapToApplicationResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJobPosting().getId())
                .jobTitle(app.getJobPosting().getTitle())
                .employerName(app.getJobPosting().getEmployer().getFullName())
                .candidateId(app.getCandidate().getId())
                .candidateName(app.getCandidate().getFullName())
                .candidateEmail(app.getCandidate().getEmail())
                .coverLetter(app.getCoverLetter())
                .cvUrl(app.getCvUrl())
                .status(app.getStatus())
                .employerFeedback(app.getEmployerFeedback())
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private JobPosting getJobOwnedByEmployer(Long jobId, Long employerId) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy tin tuyển dụng với ID: " + jobId,
                        HttpStatus.NOT_FOUND));

        if (!job.getEmployer().getId().equals(employerId)) {
            throw new AppException(
                    "Bạn không có quyền thao tác với tin tuyển dụng này",
                    HttpStatus.FORBIDDEN);
        }
        return job;
    }

    private JobPostingResponse mapToJobResponse(JobPosting job) {
        return JobPostingResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .salaryRange(job.getSalaryRange())
                .status(job.getStatus())
                .employerEmail(job.getEmployer().getEmail())
                .employerName(job.getEmployer().getFullName())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}