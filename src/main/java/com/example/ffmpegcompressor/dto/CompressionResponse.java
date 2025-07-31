package com.example.ffmpegcompressor.dto;

public class CompressionResponse {
    private boolean success;
    private String originalFileName;
    private String compressedFileName;
    private long originalSize;
    private long compressedSize;
    private double compressionRatio;
    private double spaceSavedPercentage;
    private long processingTimeMs;
    private String outputPath;
    private String fileType;

    // Default constructor
    public CompressionResponse() {}

    // Constructor
    public CompressionResponse(boolean success, String originalFileName, String compressedFileName,
                             long originalSize, long compressedSize, double compressionRatio,
                             double spaceSavedPercentage, long processingTimeMs, String outputPath, String fileType) {
        this.success = success;
        this.originalFileName = originalFileName;
        this.compressedFileName = compressedFileName;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.compressionRatio = compressionRatio;
        this.spaceSavedPercentage = spaceSavedPercentage;
        this.processingTimeMs = processingTimeMs;
        this.outputPath = outputPath;
        this.fileType = fileType;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private String originalFileName;
        private String compressedFileName;
        private long originalSize;
        private long compressedSize;
        private double compressionRatio;
        private double spaceSavedPercentage;
        private long processingTimeMs;
        private String outputPath;
        private String fileType;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder originalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
            return this;
        }

        public Builder compressedFileName(String compressedFileName) {
            this.compressedFileName = compressedFileName;
            return this;
        }

        public Builder originalSize(long originalSize) {
            this.originalSize = originalSize;
            return this;
        }

        public Builder compressedSize(long compressedSize) {
            this.compressedSize = compressedSize;
            return this;
        }

        public Builder compressionRatio(double compressionRatio) {
            this.compressionRatio = compressionRatio;
            return this;
        }

        public Builder spaceSavedPercentage(double spaceSavedPercentage) {
            this.spaceSavedPercentage = spaceSavedPercentage;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder outputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder fileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public CompressionResponse build() {
            return new CompressionResponse(success, originalFileName, compressedFileName,
                    originalSize, compressedSize, compressionRatio, spaceSavedPercentage,
                    processingTimeMs, outputPath, fileType);
        }
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getCompressedFileName() {
        return compressedFileName;
    }

    public void setCompressedFileName(String compressedFileName) {
        this.compressedFileName = compressedFileName;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public double getCompressionRatio() {
        return compressionRatio;
    }

    public void setCompressionRatio(double compressionRatio) {
        this.compressionRatio = compressionRatio;
    }

    public double getSpaceSavedPercentage() {
        return spaceSavedPercentage;
    }

    public void setSpaceSavedPercentage(double spaceSavedPercentage) {
        this.spaceSavedPercentage = spaceSavedPercentage;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
