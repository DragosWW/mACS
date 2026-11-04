package com.example.lab3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final Set<String> ALLOWED_EXT =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "mov", "webm");

    private final Path baseDir;

    public MediaController(@Value("${media.base-path:media}") String basePath) throws IOException {
        this.baseDir = Path.of(basePath).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {
            return error("File is empty");
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        ext = (ext == null) ? "" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            return error("Extension not allowed: ." + ext);
        }

        String type = file.getContentType();
        if (type == null || !(type.startsWith("image/") || type.startsWith("video/"))) {
            return error("Content-type not allowed: " + type);
        }

        String filename = UUID.randomUUID() + "." + ext;
        Path target = baseDir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mediaUrl", "/media/" + filename));
    }

    private ResponseEntity<Map<String, String>> error(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}