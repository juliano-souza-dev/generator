package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FfmpegMediaProcessorTest {
    @Test
    void previewUsesH264AacAndFaststartForJavaFxCompatibility() {
        List<String> command = FfmpegMediaProcessor.previewCommand(
            Path.of("ffmpeg.exe"),
            Path.of("source.webm"),
            Path.of("preview.mp4")
        );

        assertEquals("ffmpeg.exe", command.getFirst());
        assertTrue(command.contains("libx264"));
        assertTrue(command.contains("yuv420p"));
        assertTrue(command.contains("aac"));
        assertTrue(command.contains("+faststart"));
        assertTrue(command.contains("source.webm"));
        assertEquals("preview.mp4", command.getLast());
    }
}
