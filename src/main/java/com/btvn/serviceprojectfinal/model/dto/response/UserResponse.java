package com.btvn.serviceprojectfinal.model.dto.response;

import com.btvn.serviceprojectfinal.model.entity.enums.RoleEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private RoleEnum role;
    private Boolean isActive;
}