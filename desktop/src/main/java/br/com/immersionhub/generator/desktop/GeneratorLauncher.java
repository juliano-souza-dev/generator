package br.com.immersionhub.generator.desktop;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.model.MediaCut;
import br.com.immersionhub.generator.desktop.model.SourceMedia;
import br.com.immersionhub.generator.desktop.navigation.ScreenId;
import br.com.immersionhub.generator.desktop.timing.MediaProcessor;
import br.com.immersionhub.generator.desktop.timing.TimingModule;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class GeneratorLauncher {
    private GeneratorLauncher() {}

    public static void main(String[] args) throws Exception {
        List<String> arguments = Arrays.asList(args);

        if (arguments.contains("--smoke-test")) {
            AppDirectories.prepare();
            if (ScreenId.SOURCE == null || ScreenId.WAVE == null || ScreenId.PREPARATION == null) {
                throw new IllegalStateException("Desktop navigation contract unavailable.");
            }
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
        var waveform = processor.waveform(source, 240);
        if (waveform.size() < 20) {
            throw new IllegalStateException("Waveform smoke test returned too few points.");
        }

        MediaCut cut = processor.cut(
            source,
            250,
            1_250,
            AppDirectories.workspaceDir().resolve("timing-smoke")
        );
        if (cut.durationMs() != 1_000) {
            throw new IllegalStateException("Timing cut smoke test returned an invalid duration.");
        }
    }
}
