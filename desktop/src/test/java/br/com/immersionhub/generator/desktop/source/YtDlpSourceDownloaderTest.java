package br.com.immersionhub.generator.desktop.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YtDlpSourceDownloaderTest {
    @Test
    void retriesTransientInspectFailureUntilSuccess(@TempDir Path tempDir) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<Long> sleeps = new ArrayList<>();

        YtDlpSourceDownloader downloader = new YtDlpSourceDownloader(
            () -> tempDir.resolve("yt-dlp.exe"),
            () -> tempDir.resolve("ffmpeg").resolve("ffmpeg.exe"),
            command -> switch (calls.incrementAndGet()) {
                case 1 -> new YtDlpSourceDownloader.CommandResult(1, "HTTP Error 503: Service Unavailable");
                case 2 -> new YtDlpSourceDownloader.CommandResult(1, "temporarily unavailable");
                default -> new YtDlpSourceDownloader.CommandResult(
                    0,
                    "{\"id\":\"abc123\",\"title\":\"Cena\",\"duration\":12.5}"
                );
            },
            sleeps::add,
            new SourceDownloadDiagnostics(tempDir.resolve("source.log"))
        );

        SourceDescriptor descriptor = downloader.inspect("https://www.youtube.com/watch?v=abc123");

        assertEquals("abc123", descriptor.remoteId());
        assertEquals("Cena", descriptor.title());
        assertEquals(12_500, descriptor.durationMs());
        assertEquals(3, calls.get());
        assertEquals(List.of(350L, 700L), sleeps);
    }

    @Test
    void doesNotRetryDefinitiveFailure(@TempDir Path tempDir) {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger sleeps = new AtomicInteger();

        YtDlpSourceDownloader downloader = new YtDlpSourceDownloader(
            () -> tempDir.resolve("yt-dlp.exe"),
            () -> tempDir.resolve("ffmpeg").resolve("ffmpeg.exe"),
            command -> {
                calls.incrementAndGet();
                return new YtDlpSourceDownloader.CommandResult(1, "ERROR: Video unavailable");
            },
            millis -> sleeps.incrementAndGet(),
            new SourceDownloadDiagnostics(tempDir.resolve("source.log"))
        );

        IOException failure = assertThrows(
            IOException.class,
            () -> downloader.inspect("https://www.youtube.com/watch?v=abc123")
        );

        assertEquals("Essa fonte não está disponível para preparação.", failure.getMessage());
        assertEquals(1, calls.get());
        assertEquals(0, sleeps.get());
    }

    @Test
    void fallsBackToSecondFormatStrategy(@TempDir Path tempDir) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        List<List<String>> commands = new ArrayList<>();

        YtDlpSourceDownloader downloader = new YtDlpSourceDownloader(
            () -> tempDir.resolve("yt-dlp.exe"),
            () -> tempDir.resolve("ffmpeg").resolve("ffmpeg.exe"),
            command -> {
                commands.add(List.copyOf(command));
                if (calls.incrementAndGet() == 1) {
                    return new YtDlpSourceDownloader.CommandResult(1, "ERROR: Requested format is not available");
                }

                int outputIndex = command.indexOf("--output") + 1;
                Path output = Path.of(command.get(outputIndex).replace("%(ext)s", "mp4"));
                Files.createDirectories(output.getParent());
                Files.writeString(output, "media");
                return new YtDlpSourceDownloader.CommandResult(0, "ok");
            },
            millis -> {},
            new SourceDownloadDiagnostics(tempDir.resolve("source.log"))
        );

        Path media = downloader.download("https://www.youtube.com/watch?v=abc123", tempDir.resolve("incoming"));

        assertTrue(Files.isRegularFile(media));
        assertEquals(2, calls.get());
        assertTrue(commands.get(0).contains("--ffmpeg-location"));
        assertTrue(commands.get(0).contains("bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/best[ext=mp4]"));
        assertTrue(commands.get(1).contains("bv*+ba/b"));
    }

    @Test
    void retriesTransientDownloadFailureInsideOneUserAction(@TempDir Path tempDir) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger sleeps = new AtomicInteger();

        YtDlpSourceDownloader downloader = new YtDlpSourceDownloader(
            () -> tempDir.resolve("yt-dlp.exe"),
            () -> tempDir.resolve("ffmpeg").resolve("ffmpeg.exe"),
            command -> {
                if (calls.incrementAndGet() == 1) {
                    return new YtDlpSourceDownloader.CommandResult(1, "HTTP Error 503");
                }
                int outputIndex = command.indexOf("--output") + 1;
                Path output = Path.of(command.get(outputIndex).replace("%(ext)s", "mp4"));
                Files.createDirectories(output.getParent());
                Files.writeString(output, "media");
                return new YtDlpSourceDownloader.CommandResult(0, "ok");
            },
            millis -> sleeps.incrementAndGet(),
            new SourceDownloadDiagnostics(tempDir.resolve("source.log"))
        );

        Path media = downloader.download("https://www.youtube.com/watch?v=abc123", tempDir.resolve("incoming"));

        assertTrue(Files.isRegularFile(media));
        assertEquals(2, calls.get());
        assertEquals(1, sleeps.get());
    }
}
