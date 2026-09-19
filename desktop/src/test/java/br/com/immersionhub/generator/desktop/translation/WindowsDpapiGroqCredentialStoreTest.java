package br.com.immersionhub.generator.desktop.translation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class WindowsDpapiGroqCredentialStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsEncryptedCredentialForCurrentWindowsUser() throws Exception {
        assumeTrue(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"));

        Path file = tempDir.resolve("groq-api-key.dpapi");
        WindowsDpapiGroqCredentialStore store = new WindowsDpapiGroqCredentialStore(file);
        String secret = "gsk_qa_secret_marker_41";

        store.save(secret);

        assertTrue(Files.isRegularFile(file));
        assertFalse(Files.readString(file).contains(secret));
        assertEquals(secret, store.load().orElseThrow());

        store.clear();
        assertTrue(store.load().isEmpty());
    }
}
