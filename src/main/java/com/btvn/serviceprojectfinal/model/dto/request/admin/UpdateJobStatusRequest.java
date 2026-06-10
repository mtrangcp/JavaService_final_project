package com.btvn.serviceprojectfinal.model.dto.request.admin;

import com.btvn.serviceprojectfinal.model.entity.enums.JobStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateJobStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private JobStatusEnum status;
}