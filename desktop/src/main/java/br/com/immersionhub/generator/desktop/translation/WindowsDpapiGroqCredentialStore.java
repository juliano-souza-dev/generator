package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;
import com.sun.jna.platform.win32.Crypt32Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

public final class WindowsDpapiGroqCredentialStore implements GroqCredentialStore {
    private final Path credentialFile;

    public WindowsDpapiGroqCredentialStore() {
        this(AppDirectories.settingsDir().resolve("groq-api-key.dpapi"));
    }

    WindowsDpapiGroqCredentialStore(Path credentialFile) {
        this.credentialFile = credentialFile;
    }

    @Override
    public Optional<String> load() throws Exception {
        if (!Files.isRegularFile(credentialFile)) return Optional.empty();
        ensureWindows();

        String encoded = Files.readString(credentialFile, StandardCharsets.UTF_8).trim();
        if (encoded.isEmpty()) return Optional.empty();

        byte[] encrypted = Base64.getDecoder().decode(encoded);
        byte[] plaintext = Crypt32Util.cryptUnprotectData(encrypted);
        try {
            String key = new String(plaintext, StandardCharsets.UTF_8).trim();
            return key.isEmpty() ? Optional.empty() : Optional.of(key);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    @Override
    public void save(String apiKey) throws Exception {
        String normalized = apiKey == null ? "" : apiKey.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Chave Groq não informada.");
        ensureWindows();

        byte[] plaintext = normalized.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted;
        try {
            encrypted = Crypt32Util.cryptProtectData(plaintext);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }

        Files.createDirectories(credentialFile.getParent());
        String protectedValue = Base64.getEncoder().encodeToString(encrypted);

        Path temporary = credentialFile.resolveSibling(credentialFile.getFileName() + ".tmp");
        Files.writeString(temporary, protectedValue, StandardCharsets.UTF_8);
        try {
            Files.move(
                temporary,
                credentialFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, credentialFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void clear() throws Exception {
        Files.deleteIfExists(credentialFile);
    }

    private static void ensureWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            throw new UnsupportedOperationException(
                "A configuração persistente da Groq está disponível no aplicativo Windows."
            );
        }
    }
}
