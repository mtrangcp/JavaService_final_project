package com.btvn.serviceprojectfinal.model.dto.request.employer;

import com.btvn.serviceprojectfinal.model.entity.enums.ApplicationStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private ApplicationStatusEnum status;

    private String feedback;
}