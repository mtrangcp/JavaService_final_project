package com.btvn.serviceprojectfinal.config;

import com.btvn.serviceprojectfinal.model.entity.JobPosting;
import com.btvn.serviceprojectfinal.model.entity.User;
import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import com.btvn.serviceprojectfinal.repository.JobPostingRepository;
import com.btvn.serviceprojectfinal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final JobPostingRepository jobPostingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initSampleUsers();
        initSampleJobs();
    }

    // admin
    private void initAdmin() {
        if (userRepository.existsByEmail("admin@system.com")) {
            log.info("[INIT] Admin đã tồn tại, bỏ qua.");
            return;
        }

        User admin = User.builder()
                .fullName("System Admin")
                .email("admin@system.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(RoleEnum.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.info("[INIT] Tạo tài khoản Admin thành công.");
        log.info("[INIT] Email: admin@system.com | Password: Admin@123");
    }

    // user
    private void initSampleUsers() {
        if (!userRepository.existsByEmail("employer@test.com")) {
            User employer = User.builder()
                    .fullName("Công ty ABC")
                    .email("employer@test.com")
                    .passwordHash(passwordEncoder.encode("Test@123"))
                    .role(RoleEnum.EMPLOYER)
                    .isActive(true)
                    .build();
            userRepository.save(employer);
            log.info("[INIT] Tạo tài khoản Employer mẫu: employer@test.com | Password: Test@123");
        }

        if (!userRepository.existsByEmail("candidate@test.com")) {
            User candidate = User.builder()
                    .fullName("Nguyen Van A")
                    .email("candidate@test.com")
                    .passwordHash(passwordEncoder.encode("Test@123"))
                    .role(RoleEnum.CANDIDATE)
                    .isActive(true)
                    .build();
            userRepository.save(candidate);
            log.info("[INIT] Tạo tài khoản Candidate mẫu: candidate@test.com | Password: Test@123");
        }
    }

    // jobs
    private void initSampleJobs() {
        if (jobPostingRepository.count() > 0) {
            log.info("[INIT] Dữ liệu Job đã tồn tại, bỏ qua.");
            return;
        }

        User employer = userRepository.findByEmail("employer@test.com")
                .orElse(null);
        if (employer == null) return;

        List<JobPosting> sampleJobs = List.of(
                JobPosting.builder()
                        .employer(employer)
                        .title("Backend Developer Java Spring Boot")
                        .description("Yêu cầu 1-2 năm kinh nghiệm Spring Boot, RESTful API.")
                        .salaryRange("15,000,000 - 25,000,000 VND")
                        .status(JobStatusEnum.APPROVED)
                        .build(),

                JobPosting.builder()
                        .employer(employer)
                        .title("Frontend Developer ReactJS")
                        .description("Thành thạo ReactJS, TypeScript. Làm việc theo nhóm Agile.")
                        .salaryRange("12,000,000 - 20,000,000 VND")
                        .status(JobStatusEnum.PENDING_APPROVAL)
                        .build(),

                JobPosting.builder()
                        .employer(employer)
                        .title("Mobile Developer Flutter")
                        .description("Có kinh nghiệm Flutter, publish app lên Store.")
                        .salaryRange("18,000,000 - 30,000,000 VND")
                        .status(JobStatusEnum.DRAFT)
                        .build()
        );

        jobPostingRepository.saveAll(sampleJobs);
        log.info("[INIT] Tạo {} tin tuyển dụng mẫu thành công.", sampleJobs.size());
    }
}