# FFmpeg Video Compressor - Spring Boot Application

A Spring Boot REST API application that uses FFmpeg to compress video files with configurable quality levels.

## Features

- 🎥 Video file upload and compression
- 📊 Three compression levels (low, medium, high)
- 📈 Detailed compression statistics
- 💾 File download and cleanup
- 🔍 Service health monitoring
- 🚀 RESTful API design

## Prerequisites

Before running this application, ensure you have:

- **Java 11+** installed
- **Maven 3.6+** installed
- **FFmpeg** installed and accessible in your system PATH

### Installing FFmpeg

**Windows:**
1. Download FFmpeg from [https://ffmpeg.org/download.html](https://ffmpeg.org/download.html)
2. Extract and add to your system PATH
3. Verify: `ffmpeg -version`

**macOS:**
\`\`\`bash
brew install ffmpeg
\`\`\`

**Ubuntu/Debian:**
\`\`\`bash
sudo apt update
sudo apt install ffmpeg
\`\`\`

## Quick Start

### 1. Clone and Build

\`\`\`bash
git clone <repository-url>
cd ffmpeg-compressor
mvn clean package
\`\`\`

### 2. Run the Application

\`\`\`bash
mvn spring-boot:run
\`\`\`

The application will start on `http://localhost:8080`

### 3. Test the Service

\`\`\`bash
# Check if service is running
curl http://localhost:8080/api/video/status
\`\`\`

## API Documentation

### 1. Compress Video

**Endpoint:** `POST /api/video/compress`

**Parameters:**
- `file` (multipart): Video file to compress
- `compressionLevel` (optional): `low`, `medium`, or `high` (default: medium)

**Example:**
\`\`\`bash
curl -X POST \
  -F "file=@sample-video.mp4" \
  -F "compressionLevel=medium" \
  http://localhost:8080/api/video/compress
\`\`\`

**Response:**
\`\`\`json
{
  "success": true,
  "originalFileName": "sample-video.mp4",
  "compressedFileName": "compressed_abc123.mp4",
  "originalSize": 50000000,
  "compressedSize": 25000000,
  "compressionRatio": 0.5,
  "spaceSavedPercentage": 50.0,
  "processingTimeMs": 30000,
  "outputPath": "compressed/compressed_abc123.mp4"
}
\`\`\`

### 2. Download Compressed Video

**Endpoint:** `GET /api/video/download/{filename}`

**Example:**
\`\`\`bash
curl -O http://localhost:8080/api/video/download/compressed_abc123.mp4
\`\`\`

### 3. Delete Compressed Video

**Endpoint:** `DELETE /api/video/cleanup/{filename}`

**Example:**
\`\`\`bash
curl -X DELETE http://localhost:8080/api/video/cleanup/compressed_abc123.mp4
\`\`\`

### 4. Service Status

**Endpoint:** `GET /api/video/status`

**Example:**
\`\`\`bash
curl http://localhost:8080/api/video/status
\`\`\`

**Response:**
\`\`\`json
{
  "service": "FFmpeg Video Compressor",
  "status": "running",
  "timestamp": 1640995200000,
  "supportedLevels": ["low", "medium", "high"]
}
\`\`\`

## Compression Levels

| Level | Quality | Speed | File Size | FFmpeg Settings |
|-------|---------|-------|-----------|-----------------|
| **Low** | Lower | Fast | Larger | CRF 28, preset fast |
| **Medium** | Balanced | Medium | Balanced | CRF 23, preset medium |
| **High** | Higher | Slow | Smaller | CRF 18, preset slow |

## Configuration

Edit `src/main/resources/application.properties`:

\`\`\`properties
# Server Configuration
server.port=8080

# File Upload Limits
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# Directory Configuration
app.upload.dir=uploads
app.output.dir=compressed

# FFmpeg Path (if not in system PATH)
app.ffmpeg.path=/usr/local/bin/ffmpeg

# Logging Level
logging.level.com.example.ffmpegcompressor=INFO
\`\`\`

## Project Structure

\`\`\`
src/
├── main/
│   ├── java/com/example/ffmpegcompressor/
│   │   ├── FfmpegCompressorApplication.java    # Main application class
│   │   ├── config/
│   │   │   └── FileUploadConfig.java           # File upload configuration
│   │   ├── controller/
│   │   │   └── VideoCompressionController.java # REST API endpoints
│   │   ├── dto/
│   │   │   └── CompressionResponse.java        # Response data transfer object
│   │   └── service/
│   │       └── FFmpegService.java              # Core FFmpeg service
│   └── resources/
│       └── application.properties              # Application configuration
└── test/
    └── java/com/example/ffmpegcompressor/
        ├── FfmpegCompressorApplicationTests.java
        └── service/
            └── FFmpegServiceTest.java
\`\`\`

## Testing with Different Tools

### Using cURL

\`\`\`bash
# Compress a video
curl -X POST \
  -F "file=@test-video.mp4" \
  -F "compressionLevel=high" \
  http://localhost:8080/api/video/compress

# Download result
curl -O http://localhost:8080/api/video/download/compressed_xyz789.mp4
\`\`\`

### Using Postman

1. **POST** `http://localhost:8080/api/video/compress`
2. Set **Body** to **form-data**
3. Add key `file` (type: File) and select your video
4. Add key `compressionLevel` (type: Text) with value `medium`
5. Send request

### Using HTML Form

\`\`\`html
<!DOCTYPE html>
<html>
<head>
    <title>Video Compressor</title>
</head>
<body>
    <form action="http://localhost:8080/api/video/compress" method="post" enctype="multipart/form-data">
        <input type="file" name="file" accept="video/*" required>
        <select name="compressionLevel">
            <option value="low">Low Quality (Fast)</option>
            <option value="medium" selected>Medium Quality</option>
            <option value="high">High Quality (Slow)</option>
        </select>
        <button type="submit">Compress Video</button>
    </form>
</body>
</html>
\`\`\`

## Error Handling

The application handles various error scenarios:

- **Empty files**: Returns 400 Bad Request
- **Invalid file types**: Only accepts video files
- **FFmpeg errors**: Returns detailed error messages
- **File system errors**: Proper logging and error responses
- **Large files**: Configurable upload limits

## Performance Considerations

### File Size Limits
- Default: 500MB per file
- Configurable via `spring.servlet.multipart.max-file-size`

### Processing Time
- Depends on video size, compression level, and system specs
- Low quality: ~30% of video duration
- Medium quality: ~50% of video duration  
- High quality: ~100% of video duration

### Storage
- Input files are automatically deleted after compression
- Compressed files remain until manually deleted
- Configure cleanup policies for production use

## Troubleshooting

### Common Issues

**1. FFmpeg not found**
\`\`\`
Error: Cannot run program "ffmpeg"
\`\`\`
**Solution:** Install FFmpeg and ensure it's in your PATH, or set `app.ffmpeg.path` in application.properties

**2. File upload too large**
\`\`\`
Error: Maximum upload size exceeded
\`\`\`
**Solution:** Increase `spring.servlet.multipart.max-file-size` in application.properties

**3. Compression fails**
\`\`\`
Error: Video compression failed
\`\`\`
**Solution:** Check FFmpeg logs in application output, ensure video file is valid

### Debugging

Enable debug logging:
\`\`\`properties
logging.level.com.example.ffmpegcompressor=DEBUG
\`\`\`

Check application logs for detailed FFmpeg command output and error messages.

## Production Deployment

### Security Considerations
- Implement authentication and authorization
- Validate file types and sizes
- Sanitize file names
- Use HTTPS in production

### Performance Optimization
- Consider async processing for large files
- Implement job queues for high volume
- Add progress tracking
- Use cloud storage for files

### Monitoring
- Add health checks
- Monitor disk space
- Track compression metrics
- Set up alerts for failures

## Development

### Running Tests
\`\`\`bash
mvn test
\`\`\`

### Building for Production
\`\`\`bash
mvn clean package -Pprod
java -jar target/ffmpeg-compressor-0.0.1-SNAPSHOT.jar
\`\`\`

### Adding New Features
1. Fork the repository
2. Create a feature branch
3. Add your changes
4. Write tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Support

If you encounter any issues or have questions:

1. Check the troubleshooting section above
2. Search existing issues on GitHub
3. Create a new issue with detailed information
4. Include logs and error messages

---

**Happy video compressing! 🎬✨**
