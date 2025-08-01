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

    @Value("${app.upload.dir:${java.io.tmpdir}/ffmpeg-compressor/uploads}")
    private String uploadDir;

    @Value("${app.output.dir:${java.io.tmpdir}/ffmpeg-compressor/compressed}")
    private String outputDir;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    public CompressionResponse compressFile(MultipartFile file, String compressionLevel,
                                            String outputFormat, Integer maxWidth, Integer maxHeight) throws Exception {
        long startTime = System.currentTimeMillis();
        createDirectories();

        //just to make the returned file values unique
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueId = UUID.randomUUID().toString();
        String inputFileName = "input_" + uniqueId + fileExtension;

        String outputExtension = determineOutputExtension(file, outputFormat);
        String outputFileName = "compressed_" + uniqueId + outputExtension;

        //gets twhere the input file should be and where the output file is going.
        Path inputPath = Paths.get(uploadDir, inputFileName).toAbsolutePath();
        Path outputPath = Paths.get(outputDir, outputFileName).toAbsolutePath();

        //will be changed for a database in future
        try {
            file.transferTo(inputPath.toFile());
            logger.info("File saved successfully to: {}", inputPath);

            // Verify file was saved
            if (!Files.exists(inputPath)) {
                throw new RuntimeException("Failed to save uploaded file to: " + inputPath);
            }

            long originalSize = Files.size(inputPath);
            logger.info("Original file size: {} bytes", originalSize);
            String contentType = file.getContentType();
            String fileType = detectFileType(contentType, originalFileName);
            logger.info("Detected file type: {}", fileType);

            List<String> command = buildFFmpegCommand(inputPath.toString(), outputPath.toString(),
                    compressionLevel, fileType, maxWidth, maxHeight);

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
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("File compression failed: " + output.toString());
            }


            if (!Files.exists(outputPath)) {
                throw new RuntimeException("FFmpeg completed but output file was not created: " + outputPath);
            }

            // Get compressed file size
            long compressedSize = Files.size(outputPath);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("Compression completed. Original: {} bytes, Compressed: {} bytes", originalSize, compressedSize);

            // Clean up input file
            try {
                Files.deleteIfExists(inputPath);
                logger.info("Cleaned up input file: {}", inputPath);
            } catch (Exception e) {
                logger.warn("Failed to clean up input file: {}", inputPath, e);
            }

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

        } catch (Exception e) {
            // Clean up files in case of error
            try {
                Files.deleteIfExists(inputPath);
                Files.deleteIfExists(outputPath);
            } catch (Exception cleanupException) {
                logger.warn("Failed to clean up files after error", cleanupException);
            }
            throw e;
        }
    }

    private String detectFileType(String contentType, String fileName) {
        if (contentType != null) {
            if (contentType.startsWith("video/")) return "video";
            if (contentType.startsWith("image/")) return "image";
        }

        // Fallback to file extension
        String ext = fileName.toLowerCase();
        if (ext.matches(".*\\.(mp4|avi|mov|mkv|webm|flv|wmv)$")) return "video";
        if (ext.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|tiff|avif)$")) return "image";

        return "unknown";
    }

    private String determineOutputExtension(MultipartFile file, String outputFormat) {
        String fileType = detectFileType(file.getContentType(), file.getOriginalFilename());

        if ("image".equals(fileType)) {
            if (outputFormat != null) {
                switch (outputFormat.toLowerCase()) {
                    case "jpeg":
                    case "jpg":
                        return ".jpg";
                    case "png":
                        return ".png";
                    case "webp":
                        return ".webp";
                    case "avif":
                        return ".avif";
                    default:
                        return ".jpg"; // Default to JPEG
                }
            }
            return ".jpg"; // Default to JPEG for images
        }

        // For videos, keep original extension or default to mp4
        return getFileExtension(file.getOriginalFilename());
    }

    private List<String> buildFFmpegCommand(String inputPath, String outputPath, String compressionLevel,
                                            String fileType, Integer maxWidth, Integer maxHeight) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(inputPath);

        switch (fileType) {
            case "video":
                return buildVideoCommand(command, outputPath, compressionLevel);
            case "image":
                return buildImageCommand(command, outputPath, compressionLevel, maxWidth, maxHeight);
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }

    private List<String> buildVideoCommand(List<String> command, String outputPath, String compressionLevel) {
        command.add("-c:v");
        command.add("libx264");
        command.add("-an"); // Remove audio stream

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

    private List<String> buildImageCommand(List<String> command, String outputPath, String compressionLevel,
                                           Integer maxWidth, Integer maxHeight) {
        // Add scaling if dimensions are specified
        if (maxWidth != null || maxHeight != null) {
            command.add("-vf");
            String scaleFilter = "scale=";
            if (maxWidth != null && maxHeight != null) {
                scaleFilter += maxWidth + ":" + maxHeight + ":force_original_aspect_ratio=decrease";
            } else if (maxWidth != null) {
                scaleFilter += maxWidth + ":-1";
            } else {
                scaleFilter += "-1:" + maxHeight;
            }
            command.add(scaleFilter);
        }

        // Determine output format and quality based on file extension
        String outputExt = outputPath.substring(outputPath.lastIndexOf('.') + 1).toLowerCase();

        switch (outputExt) {
            case "jpg":
            case "jpeg":
                command.add("-q:v");
                switch (compressionLevel.toLowerCase()) {
                    case "low":
                        command.add("8"); // Lower quality, smaller file
                        break;
                    case "high":
                        command.add("2"); // Higher quality, larger file
                        break;
                    case "medium":
                    default:
                        command.add("5"); // Medium quality
                        break;
                }
                break;

            case "png":
                command.add("-compression_level");
                switch (compressionLevel.toLowerCase()) {
                    case "low":
                        command.add("1"); // Fast compression
                        break;
                    case "high":
                        command.add("9"); // Best compression
                        break;
                    case "medium":
                    default:
                        command.add("6"); // Medium compression
                        break;
                }
                break;

            case "webp":
                command.add("-quality");
                switch (compressionLevel.toLowerCase()) {
                    case "low":
                        command.add("60");
                        break;
                    case "high":
                        command.add("90");
                        break;
                    case "medium":
                    default:
                        command.add("75");
                        break;
                }
                break;

            case "avif":
                command.add("-crf");
                switch (compressionLevel.toLowerCase()) {
                    case "low":
                        command.add("35");
                        break;
                    case "high":
                        command.add("20");
                        break;
                    case "medium":
                    default:
                        command.add("28");
                        break;
                }
                break;
        }

        command.add("-y");
        command.add(outputPath);
        return command;
    }

    private void createDirectories() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        Path outputPath = Paths.get(outputDir);

        Files.createDirectories(uploadPath);
        Files.createDirectories(outputPath);

        logger.info("Created directories - Upload: {}, Output: {}", uploadPath.toAbsolutePath(), outputPath.toAbsolutePath());
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
            boolean deleted = Files.deleteIfExists(filePath);
            logger.info("File deletion result for {}: {}", fileName, deleted);
            return deleted;
        } catch (IOException e) {
            logger.error("Error deleting file: {}", fileName, e);
            return false;
        }
    }

    public File getCompressedFile(String fileName) {
        Path filePath = Paths.get(outputDir, fileName);
        File file = filePath.toFile();
        logger.info("Looking for file: {}, exists: {}", filePath.toAbsolutePath(), file.exists());
        return file.exists() ? file : null;
    }
}