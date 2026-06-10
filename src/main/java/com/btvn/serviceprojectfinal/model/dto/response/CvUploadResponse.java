package com.btvn.serviceprojectfinal.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CvUploadResponse {
    private String cvUrl;
    private String publicId;
    private String email;
    private String fullName;
}
