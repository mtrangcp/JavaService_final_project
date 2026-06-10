package com.btvn.serviceprojectfinal.service.admin;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.model.dto.request.admin.UpdateJobStatusRequest;
import com.btvn.serviceprojectfinal.model.dto.request.admin.UpdateUserRequest;
import com.btvn.serviceprojectfinal.model.dto.response.admin.JobPostingResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.PageResponse;
import com.btvn.serviceprojectfinal.model.dto.response.UserResponse;
import com.btvn.serviceprojectfinal.model.entity.JobPosting;
import com.btvn.serviceprojectfinal.model.entity.User;
import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import com.btvn.serviceprojectfinal.repository.JobPostingRepository;
import com.btvn.serviceprojectfinal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JobPostingRepository jobPostingRepository;

    // crud user
    public PageResponse<UserResponse> getAllUsers(String keyword, RoleEnum role,
                                                  int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage = userRepository.searchUsers(keyword, role, pageable);

        java.util.List<UserResponse> content = userPage.getContent()
                .stream()
                .map(this::mapToUserResponse)
                .collect(java.util.stream.Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(content)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }


    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy người dùng với ID: " + id,
                        HttpStatus.NOT_FOUND));
        return mapToUserResponse(user);
    }


    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy người dùng với ID: " + id,
                        HttpStatus.NOT_FOUND));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        userRepository.save(user);
        return mapToUserResponse(user);
    }


    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy người dùng với ID: " + id,
                        HttpStatus.NOT_FOUND));
        user.setIsActive(false);
        userRepository.save(user);
    }

    // duyet tn tuyn dung
    public PageResponse<JobPostingResponse> getAllJobs(String keyword,
                                                       JobStatusEnum status,
                                                       int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<JobPosting> jobPage = jobPostingRepository.searchJobs(keyword, status, pageable);

        java.util.List<JobPostingResponse> content = jobPage.getContent()
                .stream()
                .map(this::mapToJobResponse)
                .collect(java.util.stream.Collectors.toList());

        return PageResponse.<JobPostingResponse>builder()
                .content(content)
                .pageNumber(jobPage.getNumber())
                .pageSize(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .last(jobPage.isLast())
                .build();
    }

    //  duyệt / từ chối tin
    @Transactional
    public JobPostingResponse updateJobStatus(Long jobId, UpdateJobStatusRequest request) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy tin tuyển dụng với ID: " + jobId,
                        HttpStatus.NOT_FOUND));

        // Admin chỉ được phép APPROVE hoặc REJECT tin PENDING_APPROVAL
        if (job.getStatus() != JobStatusEnum.PENDING_APPROVAL) {
            throw new AppException(
                    "Chỉ có thể duyệt tin đang ở trạng thái PENDING_APPROVAL",
                    HttpStatus.BAD_REQUEST);
        }

        JobStatusEnum newStatus = request.getStatus();
        if (newStatus != JobStatusEnum.APPROVED && newStatus != JobStatusEnum.REJECTED) {
            throw new AppException(
                    "Admin chỉ được phép APPROVED hoặc REJECTED",
                    HttpStatus.BAD_REQUEST);
        }

        job.setStatus(newStatus);
        jobPostingRepository.save(job);
        return mapToJobResponse(job);
    }

    // mapper Stream-safe
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
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