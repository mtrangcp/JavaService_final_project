package com.btvn.serviceprojectfinal.model.dto.request.employer;

import lombok.Data;

@Data
public class UpdateJobRequest {
    private String title;
    private String description;
    private String salaryRange;
}