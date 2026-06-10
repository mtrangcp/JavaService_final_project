package com.btvn.serviceprojectfinal.model.dto.request.candidate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplyJobRequest {

    @NotNull(message = "JobId không được để trống")
    private Long jobId;

    private String coverLetter;

    private String cvUrl;
}