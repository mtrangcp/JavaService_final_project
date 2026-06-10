package com.btvn.serviceprojectfinal.model.dto.response;

import com.btvn.serviceprojectfinal.model.entity.enums.ApplicationStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String employerName;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String coverLetter;
    private String cvUrl;
    private ApplicationStatusEnum status;
    private String employerFeedback;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}