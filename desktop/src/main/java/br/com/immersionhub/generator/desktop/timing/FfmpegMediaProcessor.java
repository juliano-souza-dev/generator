package br.com.immersionhub.generator.desktop.timing;

import br.com.immersionhub.generator.desktop.infrastructure.BundledTools;
import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class FfmpegMediaProcessor implements MediaProcessor {
    private final Supplier<Path> ffmpegSupplier;
    private final PcmWaveformReducer reducer = new PcmWaveformReducer();

    public FfmpegMediaProcessor() {
        this(BundledTools::ffmpeg);
    }

    FfmpegMediaProcessor(Supplier<Path> ffmpegSupplier) {
        this.ffmpegSupplier = ffmpegSupplier;
    }

    @Override
    public List<Double> waveform(SourceMedia sourceMedia, int points) throws Exception {
        Path ffmpeg = ffmpegSupplier.get();
        Process process = new ProcessBuilder(
            ffmpeg.toString(),
            "-hide_banner",
            "-loglevel", "error",
            "-i", sourceMedia.localPath().toString(),
            "-vn",
            "-ac", "1",
            "-ar", "8000",
            "-f", "s16le",
            "pipe:1"
        ).redirectError(ProcessBuilder.Redirect.PIPE).start();

        byte[] pcm;
        try (var input = process.getInputStream()) {
            pcm = input.readAllBytes();
        }
        String error;
        try (var errorStream = process.getErrorStream()) {
            error = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException(error.isBlank() ? "Não foi possível gerar a waveform." : error.trim());
        }
        return reducer.reduce(pcm, points);
    }

    @Override
    public Path preview(SourceMedia sourceMedia, Path outputDirectory) throws Exception {
        Path dir = outputDirectory.resolve("preview").resolve(sourceMedia.sourceId());
        Files.createDirectories(dir);

        Path output = dir.resolve("preview.mp4");
        if (Files.isRegularFile(output) && Files.size(output) > 0
            && Files.getLastModifiedTime(output).toMillis() >= Files.getLastModifiedTime(sourceMedia.localPath()).toMillis()) {
            return output;
        }

        Path temporary = dir.resolve("preview.tmp.mp4");
        Files.deleteIfExists(temporary);

        Path ffmpeg = ffmpegSupplier.get();
        Process process = new ProcessBuilder(
            previewCommand(ffmpeg, sourceMedia.localPath(), temporary)
        ).redirectErrorStream(true).start();

        ByteArrayOutputStream outputLog = new ByteArrayOutputStream();
        try (var input = process.getInputStream()) {
            input.transferTo(outputLog);
        }
        int exit = process.waitFor();

        if (exit != 0 || !Files.isRegularFile(temporary) || Files.size(temporary) == 0) {
            Files.deleteIfExists(temporary);
            String detail = outputLog.toString(StandardCharsets.UTF_8).trim();
            throw new IOException(detail.isEmpty() ? "Não foi possível preparar a prévia de vídeo." : detail);
        }

        try {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
        return output;
    }

    static List<String> previewCommand(Path ffmpeg, Path input, Path output) {
        return List.of(
            ffmpeg.toString(),
            "-hide_banner",
            "-loglevel", "error",
            "-y",
            "-i", input.toString(),
            "-map", "0:v:0",
            "-map", "0:a:0?",
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-crf", "23",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            "-b:a", "128k",
            "-movflags", "+faststart",
            output.toString()
        );
    }

    @Override
    public MediaCut cut(SourceMedia sourceMedia, long startMs, long endMs, Path outputDirectory) throws Exception {
        if (startMs < 0 || endMs <= startMs || endMs > sourceMedia.durationMs()) {
            throw new IllegalArgumentException("Intervalo de recorte inválido.");
        }

        Files.createDirectories(outputDirectory);
        Path temporary = outputDirectory.resolve("scene_video.tmp.mp4");
        Path output = outputDirectory.resolve("scene_video.mp4");
        Files.deleteIfExists(temporary);

        Path ffmpeg = ffmpegSupplier.get();
        Process process = new ProcessBuilder(
            ffmpeg.toString(),
            "-hide_banner",
            "-loglevel", "error",
            "-y",
            "-i", sourceMedia.localPath().toString(),
            "-ss", seconds(startMs),
            "-t", seconds(endMs - startMs),
            "-map", "0:v:0",
            "-map", "0:a:0?",
            "-c:v", "libx264",
            "-preset", "veryfast",
            "-crf", "20",
            "-c:a", "aac",
            "-movflags", "+faststart",
            temporary.toString()
        ).redirectErrorStream(true).start();

        ByteArrayOutputStream outputLog = new ByteArrayOutputStream();
        try (var input = process.getInputStream()) {
            input.transferTo(outputLog);
        }
        int exit = process.waitFor();

        if (exit != 0 || !Files.isRegularFile(temporary) || Files.size(temporary) == 0) {
            Files.deleteIfExists(temporary);
            String detail = outputLog.toString(StandardCharsets.UTF_8).trim();
            throw new IOException(detail.isEmpty() ? "Não foi possível salvar o recorte." : detail);
        }

        Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        return new MediaCut(
            sourceMedia.sourceId(),
            sourceMedia.localPath(),
            output,
            startMs,
            endMs,
            endMs - startMs,
            Instant.now()
        );
    }

    private static String seconds(long ms) {
        return String.format(Locale.ROOT, "%.3f", ms / 1000.0);
    }
}
