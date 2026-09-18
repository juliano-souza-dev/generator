package br.com.immersionhub.generator.desktop.preparation;

import br.com.immersionhub.generator.desktop.infrastructure.BundledTools;
import br.com.immersionhub.generator.desktop.model.MediaCut;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Supplier;

public final class FfmpegTechnicalAudioExtractor implements TechnicalAudioExtractor {
    private final Supplier<Path> ffmpeg;
    public FfmpegTechnicalAudioExtractor() { this(BundledTools::ffmpeg); }
    FfmpegTechnicalAudioExtractor(Supplier<Path> ffmpeg) { this.ffmpeg = ffmpeg; }

    public Path extract(MediaCut cut, Path outputDirectory) throws Exception {
        Files.createDirectories(outputDirectory);
        Path temp = outputDirectory.resolve("scene_audio.tmp.wav");
        Path out = outputDirectory.resolve("scene_audio_16k_mono.wav");
        Files.deleteIfExists(temp);
        Process p = new ProcessBuilder(ffmpeg.get().toString(), "-hide_banner", "-loglevel", "error", "-y",
            "-i", cut.outputPath().toString(), "-vn", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", temp.toString())
            .redirectErrorStream(true).start();
        String log;
        try (var in=p.getInputStream()) { log=new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        int exit=p.waitFor();
        if(exit!=0 || !Files.isRegularFile(temp) || Files.size(temp)==0) {
            Files.deleteIfExists(temp);
            throw new IOException(log.isBlank() ? "Não foi possível preparar o áudio." : log.trim());
        }
        Files.move(temp,out,StandardCopyOption.REPLACE_EXISTING);
        return out;
    }
}
