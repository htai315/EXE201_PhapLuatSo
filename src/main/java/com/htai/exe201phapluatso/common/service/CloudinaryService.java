package com.htai.exe201phapluatso.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Upload file to Cloudinary
     * @param file The file to upload
     * @param folder The folder in Cloudinary (e.g., "avatars", "legal_docs")
     * @return The secure URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            Map params = ObjectUtils.asMap(
                "folder", "exe201_phapluatso/" + folder,
                "resource_type", "auto"
            );
            
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            String url = (String) uploadResult.get("secure_url");
            
            log.info("Uploaded file to Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new BadRequestException("Lỗi khi upload file lên Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Delete file from Cloudinary
     * @param fileUrl The full URL of the file to delete
     */
    @Async
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("cloudinary")) {
            return;
        }

        try {
            // Extract public ID from URL
            // URL format: https://res.cloudinary.com/demo/image/upload/v1570979139/exe201_phapluatso/avatars/sample.jpg
            String publicId = extractPublicId(fileUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted file from Cloudinary: {}", publicId);
            }
        } catch (IOException e) {
            log.error("Cloudinary delete failed: {}", fileUrl, e);
        }
    }

    private String extractPublicId(String url) {
        try {
            // Find "exe201_phapluatso" and everything after it, removing extension
            int startIndex = url.indexOf("exe201_phapluatso");
            if (startIndex == -1) return null;
            
            String path = url.substring(startIndex);
            int lastDot = path.lastIndexOf(".");
            if (lastDot != -1) {
                return path.substring(0, lastDot);
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}
