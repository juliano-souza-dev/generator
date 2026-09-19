package br.com.immersionhub.generator.desktop.translation;

import java.nio.file.Path;
import java.util.Objects;

public record ExternalAiPackage(Path directory, Path zipFile, Path audioFile, Path instructionsFile) {
    public ExternalAiPackage {
        directory = Objects.requireNonNull(directory).toAbsolutePath().normalize();
        zipFile = Objects.requireNonNull(zipFile).toAbsolutePath().normalize();
        audioFile = Objects.requireNonNull(audioFile).toAbsolutePath().normalize();
        instructionsFile = Objects.requireNonNull(instructionsFile).toAbsolutePath().normalize();
    }
}
