package com.btvn.serviceprojectfinal.model.dto.request.candidate;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadCvRequest {
    private MultipartFile file;
}
