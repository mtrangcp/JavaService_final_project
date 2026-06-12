package com.btvn.serviceprojectfinal.service.candidate;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.model.dto.request.ChangePasswordRequest;
import com.btvn.serviceprojectfinal.model.dto.request.ForgotPasswordRequest;
import com.btvn.serviceprojectfinal.model.dto.request.ResetPasswordRequest;
import com.btvn.serviceprojectfinal.model.dto.request.candidate.ApplyJobRequest;
import com.btvn.serviceprojectfinal.model.dto.response.ApplicationResponse;
import com.btvn.serviceprojectfinal.model.dto.response.CvUploadResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.JobPostingResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.PageResponse;
import com.btvn.serviceprojectfinal.model.entity.Application;
import com.btvn.serviceprojectfinal.model.entity.JobPosting;
import com.btvn.serviceprojectfinal.model.entity.PasswordResetOtp;
import com.btvn.serviceprojectfinal.model.entity.User;
import com.btvn.serviceprojectfinal.model.entity.enums.ApplicationStatusEnum;
import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import com.btvn.serviceprojectfinal.repository.ApplicationRepository;
import com.btvn.serviceprojectfinal.repository.JobPostingRepository;
import com.btvn.serviceprojectfinal.repository.PasswordResetOtpRepository;
import com.btvn.serviceprojectfinal.repository.UserRepository;
import com.btvn.serviceprojectfinal.service.EmailService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateService {
    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;

    private final Cloudinary cloudinary;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${otp.expiration-minutes}")
    private long otpExpirationMinutes;

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

    @Transactional
    public CvUploadResponse uploadCv(MultipartFile file) {
        User candidate = getCurrentCandidate();

        // Validate định dạng file
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new AppException(
                    "Chỉ chấp nhận file định dạng PDF",
                    HttpStatus.BAD_REQUEST);
        }

        // Validate dung lượng file (max 15MB theo NFR)
        long maxSize = 15L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new AppException(
                    "Dung lượng file không được vượt quá 15MB",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            // Upload lên Cloudinary
            String publicId = "cv/" + candidate.getId() + "_" + System.currentTimeMillis();

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id",    publicId,
                            "resource_type","raw",       // PDF không phải image
                            "folder",       "job_portal/cv",
                            "overwrite",    true
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            String uploadedPublicId = (String) uploadResult.get("public_id");

            // Lưu URL vào DB
            candidate.setCvUrl(secureUrl);
            userRepository.save(candidate);

            return CvUploadResponse.builder()
                    .cvUrl(secureUrl)
                    .publicId(uploadedPublicId)
                    .email(candidate.getEmail())
                    .fullName(candidate.getFullName())
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(
                    "Lỗi kết nối dịch vụ lưu trữ đám mây: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // =============================================
// FR-10: Đổi mật khẩu (khi đã đăng nhập)
// =============================================
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentCandidate();

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(
                    "Mật khẩu hiện tại không chính xác",
                    HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra mật khẩu mới và xác nhận khớp nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(
                    "Mật khẩu mới và xác nhận mật khẩu không khớp",
                    HttpStatus.BAD_REQUEST);
        }

        // Không được đặt mật khẩu mới trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(
                    "Mật khẩu mới không được trùng mật khẩu hiện tại",
                    HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

            // Xóa toàn bộ OTP cũ của email này
            otpRepository.deleteAllByEmail(request.getEmail());

            // Tạo OTP 6 số ngẫu nhiên
            String otpCode = String.format("%06d",
                    new java.util.Random().nextInt(999999));

            PasswordResetOtp otp = PasswordResetOtp.builder()
                    .email(request.getEmail())
                    .otpCode(otpCode)
                    .expiredAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                    .isUsed(false)
                    .build();

            otpRepository.save(otp);

            // Gửi email bất đồng bộ
            emailService.sendOtpEmail(
                    user.getEmail(),
                    user.getFullName(),
                    otpCode,
                    otpExpirationMinutes);
        });
    }

    // =============================================
// FR-10: Quên mật khẩu — Bước 2: Xác nhận OTP & Đặt lại mật khẩu
// =============================================
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Kiểm tra mật khẩu mới và xác nhận khớp
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(
                    "Mật khẩu mới và xác nhận mật khẩu không khớp",
                    HttpStatus.BAD_REQUEST);
        }

        // Tìm OTP hợp lệ
        PasswordResetOtp otp = otpRepository
                .findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new AppException(
                        "Mã OTP không hợp lệ hoặc đã được sử dụng",
                        HttpStatus.BAD_REQUEST));

        // Kiểm tra đúng mã OTP
        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            throw new AppException("Mã OTP không chính xác", HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra OTP còn hạn
        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new AppException(
                    "Mã OTP đã hết hạn, vui lòng yêu cầu mã mới",
                    HttpStatus.BAD_REQUEST);
        }

        // Đánh dấu OTP đã dùng
        otp.setIsUsed(true);
        otpRepository.save(otp);

        // Cập nhật mật khẩu mới
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(
                        "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
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