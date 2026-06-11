package com.btvn.serviceprojectfinal.service;

import com.btvn.serviceprojectfinal.exception.AppException;
import com.btvn.serviceprojectfinal.model.dto.request.admin.UpdateJobStatusRequest;
import com.btvn.serviceprojectfinal.model.dto.response.UserResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.JobPostingResponse;
import com.btvn.serviceprojectfinal.model.dto.response.admin.PageResponse;
import com.btvn.serviceprojectfinal.model.entity.JobPosting;
import com.btvn.serviceprojectfinal.model.entity.User;
import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import com.btvn.serviceprojectfinal.repository.JobPostingRepository;
import com.btvn.serviceprojectfinal.repository.UserRepository;
import com.btvn.serviceprojectfinal.service.admin.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private AdminService adminService;

    private User mockEmployer;
    private JobPosting mockJob;

    @BeforeEach
    void setUp() {
        mockEmployer = User.builder()
                .id(1L)
                .fullName("Cong ty ABC")
                .email("employer@test.com")
                .role(RoleEnum.EMPLOYER)
                .isActive(true)
                .build();

        mockJob = JobPosting.builder()
                .id(1L)
                .employer(mockEmployer)
                .title("Java Developer")
                .description("Spring Boot experience required")
                .salaryRange("15M - 25M")
                .status(JobStatusEnum.PENDING_APPROVAL)
                .build();
    }

    // ===== TEST 6 =====
    @Test
    @DisplayName("ST-06: Lấy danh sách user thành công → trả về PageResponse")
    void getAllUsers_ValidRequest_ReturnPageResponse() {
        // Arrange
        User mockCandidate = User.builder()
                .id(2L).fullName("Nguyen Van A")
                .email("candidate@test.com")
                .role(RoleEnum.CANDIDATE)
                .isActive(true).build();

        Page<User> mockPage = new PageImpl<>(List.of(mockCandidate));
        when(userRepository.searchUsers(any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        PageResponse<UserResponse> response =
                adminService.getAllUsers(null, null, 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals("Nguyen Van A", response.getContent().get(0).getFullName());
        assertEquals(RoleEnum.CANDIDATE, response.getContent().get(0).getRole());
    }

    // ===== TEST 7 =====
    @Test
    @DisplayName("ST-07: Lấy user theo ID không tồn tại → 404 Not Found")
    void getUserById_NotFound_ThrowAppException404() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
                () -> adminService.getUserById(99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("99"));
    }

    // ===== TEST 8 =====
    @Test
    @DisplayName("ST-08: Admin duyệt tin PENDING_APPROVAL → APPROVED thành công")
    void updateJobStatus_PendingToApproved_ReturnUpdatedJob() {
        // Arrange
        UpdateJobStatusRequest request = new UpdateJobStatusRequest();
        request.setStatus(JobStatusEnum.APPROVED);

        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(mockJob));
        when(jobPostingRepository.save(any(JobPosting.class))).thenReturn(mockJob);

        // Act
        JobPostingResponse response = adminService.updateJobStatus(1L, request);

        // Assert
        assertNotNull(response);
        verify(jobPostingRepository, times(1)).save(any(JobPosting.class));
        assertEquals(JobStatusEnum.APPROVED, mockJob.getStatus());
    }

    // ===== TEST 9 =====
    @Test
    @DisplayName("ST-09: Admin duyệt tin không ở PENDING_APPROVAL → 400 Bad Request")
    void updateJobStatus_NotPending_ThrowAppException400() {
        // Arrange
        mockJob.setStatus(JobStatusEnum.APPROVED); // Đã duyệt rồi
        UpdateJobStatusRequest request = new UpdateJobStatusRequest();
        request.setStatus(JobStatusEnum.REJECTED);

        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(mockJob));

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
                () -> adminService.updateJobStatus(1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(jobPostingRepository, never()).save(any());
    }

    // ===== TEST 10 =====
    @Test
    @DisplayName("ST-10: Vô hiệu hóa user thành công → isActive = false")
    void deactivateUser_ValidId_SetIsActiveFalse() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockEmployer));
        when(userRepository.save(any(User.class))).thenReturn(mockEmployer);

        // Act
        adminService.deactivateUser(1L);

        // Assert
        assertFalse(mockEmployer.getIsActive());
        verify(userRepository, times(1)).save(mockEmployer);
    }
}