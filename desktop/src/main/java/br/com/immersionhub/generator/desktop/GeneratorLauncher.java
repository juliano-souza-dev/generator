package br.com.immersionhub.generator.desktop;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.navigation.ScreenId;
import br.com.immersionhub.generator.desktop.preparation.AlignedMaterial;
import br.com.immersionhub.generator.desktop.preparation.PreparationModule;
import br.com.immersionhub.generator.desktop.timing.MediaProcessor;
import br.com.immersionhub.generator.desktop.timing.TimingModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class GeneratorLauncher {
    private GeneratorLauncher() {}

    public static void main(String[] args) throws Exception {
        List<String> arguments = Arrays.asList(args);

        if (arguments.contains("--smoke-test")) {
            AppDirectories.prepare();
            if (ScreenId.SOURCE == null
                || ScreenId.WAVE == null
                || ScreenId.PREPARATION == null
                || ScreenId.TRANSLATION == null) {
                throw new IllegalStateException("Desktop navigation contract unavailable.");
            }
            return;
        }

        if (arguments.contains("--installer-lock-smoke")) {
            AppDirectories.prepare();
            Thread.sleep(180_000L);
            return;
        }

        int timingIndex = arguments.indexOf("--timing-smoke");
        if (timingIndex >= 0) {
            if (timingIndex + 1 >= arguments.size()) {
                throw new IllegalArgumentException("--timing-smoke requer um arquivo de mídia.");
            }
            runTimingSmoke(Path.of(arguments.get(timingIndex + 1)));
            return;
        }

        int preparationIndex = arguments.indexOf("--preparation-smoke");
        if (preparationIndex >= 0) {
            if (preparationIndex + 2 >= arguments.size()) {
                throw new IllegalArgumentException("--preparation-smoke requer mídia e duração em ms.");
            }
            runPreparationSmoke(
                Path.of(arguments.get(preparationIndex + 1)),
                Long.parseLong(arguments.get(preparationIndex + 2))
            );
            return;
        }

        GeneratorDesktopApp.launchApp(args);
    }

    private static void runTimingSmoke(Path sourcePath) throws Exception {
        AppDirectories.prepare();
        SourceMedia source = new SourceMedia(
            "timing-smoke",
            "https://local.invalid/timing-smoke",
            sourcePath,
            "Timing Smoke",
            2_000,
            Instant.now(),
            false
        );

        MediaProcessor processor = TimingModule.createProcessor();
        Path timingSmokeDir = AppDirectories.workspaceDir().resolve("timing-smoke");

        Path preview = processor.preview(source, timingSmokeDir);
        if (!Files.isRegularFile(preview) || Files.size(preview) == 0 || preview.equals(source.localPath())) {
            throw new IllegalStateException("Timing preview smoke did not produce a reusable derived video.");
        }

        var waveform = processor.waveform(source, 240);
        if (waveform.size() < 20) {
            throw new IllegalStateException("Waveform smoke test returned too few points.");
        }

        MediaCut cut = processor.cut(
            source,
            250,
            1_250,
            timingSmokeDir
        );
        if (cut.durationMs() != 1_000) {
            throw new IllegalStateException("Timing cut smoke test returned an invalid duration.");
        }
    }

    private static void runPreparationSmoke(Path mediaPath, long durationMs) throws Exception {
        AppDirectories.prepare();
        Path sourcePath = mediaPath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(sourcePath) || durationMs <= 0) {
            throw new IllegalArgumentException("Mídia de smoke inválida.");
        }

        Path smokeDir = AppDirectories.workspaceDir().resolve("preparation-smoke");
        Files.createDirectories(smokeDir);
        Path approvedCut = smokeDir.resolve("approved-cut" + extensionOf(sourcePath));
        Files.copy(sourcePath, approvedCut, StandardCopyOption.REPLACE_EXISTING);

        MediaCut cut = new MediaCut(
            "preparation-smoke",
            sourcePath,
            approvedCut,
            0,
            durationMs,
            durationMs,
            Instant.EPOCH
        );

        var pipeline = PreparationModule.createPipeline();
        AlignedMaterial first = pipeline.prepare(cut, ignored -> {});
        AlignedMaterial second = pipeline.prepare(cut, ignored -> {});

        if (!first.id().equals(second.id())) {
            throw new IllegalStateException("Preparation cache identity was not reused.");
        }
        if (first.transcription().text().isBlank() || first.words().isEmpty()) {
            throw new IllegalStateException("Preparation smoke did not produce transcript and timed words.");
        }
        for (var word : first.words()) {
            if (word.startMs() < 0 || word.endMs() <= word.startMs() || word.endMs() > durationMs) {
                throw new IllegalStateException("Preparation smoke produced invalid word timing.");
            }
        }
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int index = name.lastIndexOf('.');
        return index < 0 ? ".media" : name.substring(index);
    }
}
