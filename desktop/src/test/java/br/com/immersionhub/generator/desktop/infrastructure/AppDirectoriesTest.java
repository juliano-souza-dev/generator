package br.com.immersionhub.generator.desktop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppDirectoriesTest {
    @Test
    void preparesPersistentDesktopDirectories(@TempDir Path tempDir) {
        String original = System.getProperty("ihub.data.dir");
        try {
            System.setProperty("ihub.data.dir", tempDir.toString());
            AppDirectories.prepare();
            assertTrue(Files.isDirectory(AppDirectories.sourceCacheDir()));
            assertTrue(Files.isDirectory(AppDirectories.projectsDir()));
            assertTrue(Files.isDirectory(AppDirectories.logsDir()));
            assertTrue(Files.isDirectory(AppDirectories.settingsDir()));
        } finally {
            if (original == null) System.clearProperty("ihub.data.dir");
            else System.setProperty("ihub.data.dir", original);
        }
    }
}
