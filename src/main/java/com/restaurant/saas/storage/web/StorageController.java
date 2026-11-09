package com.restaurant.saas.storage.web;

import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.storage.service.StorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<FileSystemResource> getFile(@PathVariable String key) throws IOException {
        Path path = storageService.resolvePath(key);
        if (!Files.exists(path)) {
            throw new NotFoundException("File " + key + " not found");
        }
        MediaType mediaType = MediaType.parseMediaType(
            java.nio.file.Files.probeContentType(path));
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
            .body(new FileSystemResource(path));
    }
}
