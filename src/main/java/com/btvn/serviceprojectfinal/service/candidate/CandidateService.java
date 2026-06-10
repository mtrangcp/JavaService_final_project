package com.btvn.serviceprojectfinal.service.candidate;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.model.dto.request.candidate.ApplyJobRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;

    private User getCurrentCandidate() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));
    }

    // FR-07: Nộp hồ sơ ứng tuyển
    @Transactional
    public ApplicationResponse applyJob(ApplyJobRequest request) {
        User candidate = getCurrentCandidate();

        JobPosting job = jobPostingRepository.findById(request.getJobId())
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy tin tuyển dụng với ID: " + request.getJobId(),
                        HttpStatus.NOT_FOUND));


        if (job.getStatus() != JobStatusEnum.APPROVED) {
            throw new AppException(
                    "Tin tuyển dụng này hiện không nhận hồ sơ",
                    HttpStatus.CONFLICT);
        }

        if (applicationRepository.existsByCandidateIdAndJobPostingId(
                candidate.getId(), job.getId())) {
            throw new AppException(
                    "Bạn đã nộp hồ sơ vào tin tuyển dụng này rồi",
                    HttpStatus.CONFLICT);
        }

        Application application = Application.builder()
                .candidate(candidate)
                .jobPosting(job)
                .coverLetter(request.getCoverLetter())
                .cvUrl(request.getCvUrl())
                .status(ApplicationStatusEnum.PENDING)
                .build();

        applicationRepository.save(application);
        return mapToApplicationResponse(application);
    }

    // FR-07: Tìm kiếm tin tuyển dụng (chỉ lấy APPROVED)
    public PageResponse<JobPostingResponse> searchJobs(String keyword,
                                                       int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<JobPosting> jobPage = jobPostingRepository
                .searchJobs(keyword, JobStatusEnum.APPROVED, pageable);

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

    // ds hồ sơ đã nộp
    public PageResponse<ApplicationResponse> getMyApplications(
            ApplicationStatusEnum status, int page, int size) {
        User candidate = getCurrentCandidate();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("appliedAt").descending());

        Page<Application> appPage = applicationRepository
                .findByCandidateId(candidate.getId(), status, pageable);

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

    public ApplicationResponse getMyApplicationById(Long applicationId) {
        User candidate = getCurrentCandidate();
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy hồ sơ với ID: " + applicationId,
                        HttpStatus.NOT_FOUND));

        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new AppException(
                    "Bạn không có quyền xem hồ sơ này",
                    HttpStatus.FORBIDDEN);
        }

        return mapToApplicationResponse(application);
    }

    //
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