package com.btvn.serviceprojectfinal.model.dto.request.employer;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateJobRequest {
    @Size(min = 5, max = 200, message = "Tiêu đề phải từ 5 đến 200 ký tự")
    private String title;

    @Size(min = 10, message = "Mô tả phải có ít nhất 10 ký tự")
    private String description;

    private String salaryRange;
}