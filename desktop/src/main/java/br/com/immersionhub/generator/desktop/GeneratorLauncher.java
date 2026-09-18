package br.com.immersionhub.generator.desktop;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import br.com.immersionhub.generator.desktop.navigation.ScreenId;

import java.util.Arrays;

public final class GeneratorLauncher {
    private GeneratorLauncher() {}

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--smoke-test")) {
            AppDirectories.prepare();
            if (ScreenId.SOURCE == null || ScreenId.WAVE == null) {
                throw new IllegalStateException("Desktop navigation contract unavailable.");
            }
            return;
        }
        GeneratorDesktopApp.launchApp(args);
    }
}
