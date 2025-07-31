package com.example.ffmpegcompressor.service;

import com.example.ffmpegcompressor.dto.CompressionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FFmpegService {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegService.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.output.dir:compressed}")
    private String outputDir;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    public CompressionResponse compressFile(MultipartFile file, String compressionLevel) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Create directories if they don't exist
        createDirectories();
        
        // Save uploaded file
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueId = UUID.randomUUID().toString();
        String inputFileName = "input_" + uniqueId + fileExtension;
        String outputFileName = "compressed_" + uniqueId + fileExtension;
        
        Path inputPath = Paths.get(uploadDir, inputFileName);
        Path outputPath = Paths.get(outputDir, outputFileName);
        
        // Save uploaded file to disk
        file.transferTo(inputPath.toFile());
        long originalSize = Files.size(inputPath);

        // Detect file type
        String contentType = file.getContentType();
        String fileType = detectFileType(contentType, originalFileName);
        
        // Build FFmpeg command
        List<String> command = buildFFmpegCommand(inputPath.toString(), outputPath.toString(), compressionLevel, fileType);
        
        // Execute FFmpeg command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("FFmpeg output: {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("FFmpeg failed with exit code: {}", exitCode);
            logger.error("FFmpeg output: {}", output.toString());
            throw new RuntimeException("Video compression failed: " + output.toString());
        }
        
        // Get compressed file size
        long compressedSize = Files.size(outputPath);
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Clean up input file
        Files.deleteIfExists(inputPath);
        
        // Calculate compression statistics
        double compressionRatio = (double) compressedSize / originalSize;
        double spaceSavedPercentage = ((double) (originalSize - compressedSize) / originalSize) * 100;
        
        return CompressionResponse.builder()
                .success(true)
                .originalFileName(originalFileName)
                .compressedFileName(outputFileName)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .compressionRatio(compressionRatio)
                .spaceSavedPercentage(spaceSavedPercentage)
                .processingTimeMs(processingTime)
                .outputPath(outputPath.toString())
                .fileType(fileType)
                .build();
    }

    private String detectFileType(String contentType, String fileName) {
        if (contentType != null) {
            if (contentType.startsWith("video/")) return "video";
            if (contentType.startsWith("audio/")) return "audio";
            if (contentType.startsWith("image/")) return "image";
        }

        // Fallback to file extension
        String ext = fileName.toLowerCase();
        if (ext.matches(".*\\.(mp4|avi|mov|mkv|webm|flv|wmv)$")) return "video";
        if (ext.matches(".*\\.(mp3|wav|flac|aac|ogg|m4a)$")) return "audio";
        if (ext.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$")) return "image";

        return "unknown";
    }
    
    private List<String> buildFFmpegCommand(String inputPath, String outputPath, String compressionLevel, String fileType) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(inputPath);
        
        switch (fileType) {
            case "video":
                return buildVideoCommand(command, outputPath, compressionLevel);
            case "audio":
                return buildAudioCommand(command, outputPath, compressionLevel);
            case "image":
                return buildImageCommand(command, outputPath, compressionLevel);
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }

    private List<String> buildVideoCommand(List<String> command, String outputPath, String compressionLevel) {
        command.add("-c:v");
        command.add("libx264");
        command.add("-c:a");
        command.add("aac");

        switch (compressionLevel.toLowerCase()) {
            case "low":
                command.add("-crf"); command.add("28");
                command.add("-preset"); command.add("fast");
                break;
            case "high":
                command.add("-crf"); command.add("18");
                command.add("-preset"); command.add("slow");
                break;
            case "medium":
            default:
                command.add("-crf"); command.add("23");
                command.add("-preset"); command.add("medium");
                break;
        }

        command.add("-y");
        command.add(outputPath);
        return command;
    }

    private List<String> buildAudioCommand(List<String> command, String outputPath, String compressionLevel) {
        command.add("-c:a");
        command.add("aac");

        switch (compressionLevel.toLowerCase()) {
            case "low":
                command.add("-b:a"); command.add("128k");
                break;
            case "high":
                command.add("-b:a"); command.add("320k");
                break;
            case "medium":
            default:
                command.add("-b:a"); command.add("192k");
                break;
        }

        command.add("-y");
        command.add(outputPath);
        return command;
    }

    private List<String> buildImageCommand(List<String> command, String outputPath, String compressionLevel) {
        // For images, we'll convert to JPEG with quality settings
        command.add("-q:v");

        switch (compressionLevel.toLowerCase()) {
            case "low":
                command.add("8"); // Lower quality
                break;
            case "high":
                command.add("2"); // Higher quality
                break;
            case "medium":
            default:
                command.add("5"); // Medium quality
                break;
        }

        command.add("-y");
        command.add(outputPath);
        return command;
    }
    
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        Files.createDirectories(Paths.get(outputDir));
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return ".mp4";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
    
    public boolean deleteCompressedFile(String fileName) {
        try {
            Path filePath = Paths.get(outputDir, fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Error deleting file: {}", fileName, e);
            return false;
        }
    }
    
    public File getCompressedFile(String fileName) {
        Path filePath = Paths.get(outputDir, fileName);
        File file = filePath.toFile();
        return file.exists() ? file : null;
    }
}
