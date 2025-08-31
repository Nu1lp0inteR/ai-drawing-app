// 文件路径: src/main/java/com/aidrawing/backend/controller/ImageController.java

package com.aidrawing.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/api/v1/images")
public class ImageController {

    private final Path storageLocation;

    // We use @Value to inject the property from application.properties.
    // Ensure the name "file.storage.path" matches exactly.
    // 我们使用 @Value 注解来注入 application.properties 中的配置项。
    // 确保这里的名称 "file.storage.path" 完全匹配。
    public ImageController(@Value("${file.storage.path}") String storagePath) {
        // Initialize the storage location from the configured path.
        // 从配置的路径初始化存储位置。
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = storageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                // Return the file with the correct content type.
                // 以正确的内容类型返回文件。
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                        .body(resource);
            } else {
                // If the file is not found, return a 404 error.
                // 如果文件未找到，返回404错误。
                System.err.println("Could not find or read file: " + filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            System.err.println("Error creating URL for file: " + filename);
            return ResponseEntity.badRequest().build();
        }
    }
}
