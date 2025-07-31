package com.example.ffmpegcompressor.controller;

import com.example.ffmpegcompressor.dto.CompressionResponse;
import com.example.ffmpegcompressor.service.FFmpegService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@CrossOrigin(origins = "*")
public class VideoCompressionController {

    private static final Logger logger = LoggerFactory.getLogger(VideoCompressionController.class);

    @Autowired
    private FFmpegService ffmpegService;

    @PostMapping("/compress")
    public ResponseEntity<?> compressFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "compressionLevel", defaultValue = "medium") String compressionLevel) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }
            
            // Remove the video-only validation
            // Replace with:
            String fileType = detectFileType(file.getContentType(), file.getOriginalFilename());
            if ("unknown".equals(fileType)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Unsupported file type. Supported: video, audio, image files"));
            }
            
            // Validate compression level
            if (!isValidCompressionLevel(compressionLevel)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid compression level. Use: low, medium, or high"));
            }
            
            logger.info("Starting compression for file: {} with level: {}", file.getOriginalFilename(), compressionLevel);
            
            CompressionResponse response = ffmpegService.compressFile(file, compressionLevel);
            
            logger.info("Compression completed successfully for file: {}", file.getOriginalFilename());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error compressing video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Compression failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadCompressedVideo(@PathVariable String filename) {
        try {
            File file = ffmpegService.getCompressedFile(filename);
            
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Error downloading file: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/cleanup/{filename}")
    public ResponseEntity<?> deleteCompressedVideo(@PathVariable String filename) {
        try {
            boolean deleted = ffmpegService.deleteCompressedFile(filename);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("message", deleted ? "File deleted successfully" : "File not found");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting file: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error deleting file: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "FFmpeg File Compressor");
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        status.put("supportedTypes", new String[]{"video", "audio", "image"});
        
        return ResponseEntity.ok(status);
    }
    
    private boolean isValidCompressionLevel(String level) {
        return level != null && (level.equalsIgnoreCase("low") || 
                                level.equalsIgnoreCase("medium") || 
                                level.equalsIgnoreCase("high"));
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    private String detectFileType(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.startsWith("video/")) return "video";
            if (contentType.startsWith("audio/")) return "audio";
            if (contentType.startsWith("image/")) return "image";
        }

        // Fallback to file extension
        String ext = filename.toLowerCase();
        if (ext.matches(".*\\.(mp4|avi|mov|mkv|webm|flv|wmv)$")) return "video";
        if (ext.matches(".*\\.(mp3|wav|flac|aac|ogg|m4a)$")) return "audio";
        if (ext.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$")) return "image";

        return "unknown";
    }
}
