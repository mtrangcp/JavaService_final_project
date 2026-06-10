package com.btvn.serviceprojectfinal.model.dto.request.admin;

import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private String fullName;

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean isActive;

    private RoleEnum role;
}
