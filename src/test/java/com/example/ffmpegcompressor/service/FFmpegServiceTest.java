package com.example.ffmpegcompressor.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "app.upload.dir=test-uploads",
    "app.output.dir=test-compressed",
    "app.ffmpeg.path=ffmpeg"
})
class FFmpegServiceTest {

    @Test
    void testServiceInitialization() {
        // Basic test to ensure service can be initialized
        // Add more comprehensive tests as needed
    }
}
