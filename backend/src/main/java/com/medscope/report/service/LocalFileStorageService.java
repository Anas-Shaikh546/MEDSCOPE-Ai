package com.medscope.report.service;

import com.medscope.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local-disk implementation. Everything that makes report storage safe
 * lives here: validation pipeline, UUID naming, and the path-traversal
 * guard - so ReportService never has to think about any of it.
 */
@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final String ALLOWED_EXTENSION = ".pdf";
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final byte[] PDF_MAGIC_BYTES = "%PDF-".getBytes();

    private final Path uploadRoot;

    public LocalFileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadRoot, e);
        }
    }

    @Override
    public StoredFile store(Long userId, MultipartFile file) {
        validate(file);

        String storedFilename = UUID.randomUUID() + ALLOWED_EXTENSION;

        // The user's own filename never touches the filesystem path -
        // it is metadata only. Path is entirely server-generated:
        // uploads/{userId}/{uuid}.pdf
        Path userDir = uploadRoot.resolve(String.valueOf(userId)).normalize();
        assertInsideUploadRoot(userDir);

        Path destination = userDir.resolve(storedFilename).normalize();
        assertInsideUploadRoot(destination);

        try {
            Files.createDirectories(userDir);
            file.transferTo(destination);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save uploaded file", e);
        }

        log.info("Report file stored: userId={}, storedFilename={}", userId, storedFilename);

        // Stored as a path relative to the upload root, not an absolute
        // machine path - see 3.21, never expose/persist a raw filesystem path.
        String relativePath = uploadRoot.relativize(destination).toString();

        return new StoredFile(storedFilename, relativePath, file.getSize());
    }

    @Override
    public byte[] read(String filePath) {
        Path resolved = uploadRoot.resolve(filePath).normalize();
        assertInsideUploadRoot(resolved);

        if (!Files.exists(resolved)) {
            throw new IllegalStateException("Stored file is missing on disk: " + filePath);
        }

        try {
            return Files.readAllBytes(resolved);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored file", e);
        }
    }

    @Override
    public void delete(String filePath) {
        Path resolved = uploadRoot.resolve(filePath).normalize();
        assertInsideUploadRoot(resolved);

        try {
            Files.deleteIfExists(resolved);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", filePath);
        }
    }

    // ---- validation pipeline: size -> extension -> MIME -> PDF signature ----

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds the maximum allowed size of 10 MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        if (!ALLOWED_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        if (!hasPdfSignature(file)) {
            throw new BadRequestException("File does not appear to be a valid PDF");
        }
    }

    private boolean hasPdfSignature(MultipartFile file) {
        byte[] header = new byte[PDF_MAGIC_BYTES.length];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(header);
            if (read < PDF_MAGIC_BYTES.length) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        for (int i = 0; i < PDF_MAGIC_BYTES.length; i++) {
            if (header[i] != PDF_MAGIC_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

    // ---- path traversal guard ----

    private void assertInsideUploadRoot(Path candidate) {
        if (!candidate.startsWith(uploadRoot)) {
            throw new IllegalStateException("Resolved path escapes the configured upload directory");
        }
    }
}