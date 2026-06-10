package com.btvn.serviceprojectfinal.model.dto.response.admin;

import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JobPostingResponse {
    private Long id;
    private String title;
    private String description;
    private String salaryRange;
    private JobStatusEnum status;
    private String employerEmail;
    private String employerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}