package br.com.immersionhub.generator.desktop.translation;

import br.com.immersionhub.generator.desktop.infrastructure.AppDirectories;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class WindowsDpapiGroqCredentialStore implements GroqCredentialStore {
    private static final String PROTECT_SCRIPT = """
        $payload = [Console]::In.ReadToEnd()
        $bytes = [Text.Encoding]::UTF8.GetBytes($payload)
        $protected = [Security.Cryptography.ProtectedData]::Protect(
            $bytes, $null, [Security.Cryptography.DataProtectionScope]::CurrentUser
        )
        [Console]::Out.Write([Convert]::ToBase64String($protected))
        """;

    private static final String UNPROTECT_SCRIPT = """
        $payload = [Console]::In.ReadToEnd().Trim()
        $protected = [Convert]::FromBase64String($payload)
        $bytes = [Security.Cryptography.ProtectedData]::Unprotect(
            $protected, $null, [Security.Cryptography.DataProtectionScope]::CurrentUser
        )
        [Console]::Out.Write([Text.Encoding]::UTF8.GetString($bytes))
        """;

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

        String encrypted = Files.readString(credentialFile, StandardCharsets.UTF_8).trim();
        if (encrypted.isEmpty()) return Optional.empty();

        String key = powershell(UNPROTECT_SCRIPT, encrypted).trim();
        return key.isEmpty() ? Optional.empty() : Optional.of(key);
    }

    @Override
    public void save(String apiKey) throws Exception {
        String normalized = apiKey == null ? "" : apiKey.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Chave Groq não informada.");
        ensureWindows();

        Files.createDirectories(credentialFile.getParent());
        String protectedValue = powershell(PROTECT_SCRIPT, normalized).trim();
        if (protectedValue.isEmpty()) {
            throw new IOException("Não foi possível proteger a configuração da Groq.");
        }

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

    private static String powershell(String script, String stdin) throws Exception {
        Process process = new ProcessBuilder(
            "powershell.exe",
            "-NoLogo",
            "-NoProfile",
            "-NonInteractive",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            script
        ).start();

        try (var output = process.getOutputStream()) {
            output.write(stdin.getBytes(StandardCharsets.UTF_8));
        }

        boolean finished = process.waitFor(20, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Tempo esgotado ao proteger a configuração da Groq.");
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IOException("Não foi possível acessar a configuração protegida da Groq.");
        }
        return stdout;
    }
}
