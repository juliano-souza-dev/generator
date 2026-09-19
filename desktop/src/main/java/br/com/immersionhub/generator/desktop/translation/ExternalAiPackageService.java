package br.com.immersionhub.generator.desktop.translation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ExternalAiPackageService {
    private static final String AUDIO_NAME = "scene_audio_16k_mono.wav";
    private static final String INSTRUCTIONS_NAME = "INSTRUCOES_EXTERNAL_AI.txt";

    private final TranslationDocumentCodec codec = new TranslationDocumentCodec();

    public ExternalAiPackage prepare(
        TranslationMaterial base,
        Path technicalAudio,
        Path translationDirectory
    ) throws Exception {
        if (!Files.isRegularFile(technicalAudio)) {
            throw new IOException("Áudio preparado não encontrado.");
        }

        Path packageDir = translationDirectory.resolve("external_ai_package");
        Files.createDirectories(packageDir);

        Path audio = packageDir.resolve(AUDIO_NAME);
        Files.copy(technicalAudio, audio, StandardCopyOption.REPLACE_EXISTING);

        Path instructions = packageDir.resolve(INSTRUCTIONS_NAME);
        Files.writeString(instructions, instructions(base), StandardCharsets.UTF_8);

        Path zip = translationDirectory.resolve("external_ai_package.zip");
        Path temporary = translationDirectory.resolve("external_ai_package.tmp.zip");
        Files.deleteIfExists(temporary);

        try (ZipOutputStream output = new ZipOutputStream(
            new BufferedOutputStream(Files.newOutputStream(temporary))
        )) {
            add(output, audio, AUDIO_NAME);
            add(output, instructions, INSTRUCTIONS_NAME);
        }
        Files.move(temporary, zip, StandardCopyOption.REPLACE_EXISTING);

        return new ExternalAiPackage(packageDir, zip, audio, instructions);
    }

    public TranslationMaterial importReturn(Path file, TranslationMaterial expectedBase) throws Exception {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Retorno externo não encontrado.");
        }

        String lower = file.getFileName().toString().toLowerCase();
        String json;
        if (lower.endsWith(".json")) {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } else if (lower.endsWith(".zip")) {
            json = readJsonFromZip(file);
        } else {
            throw new IllegalArgumentException("Selecione um JSON ou ZIP devolvido pela IA externa.");
        }

        return codec.parseAndValidate(json, expectedBase, TranslationSource.EXTERNAL);
    }

    private String instructions(TranslationMaterial base) throws Exception {
        return """
            IHUB GENERATOR — TRADUÇÃO PT-BR

            Ouça o áudio completo antes de concluir.
            Traduza cada cue para português brasileiro natural e contextual.
            Preencha SOMENTE o campo "pt" de cada cue.
            Não altere order, timings, speaker, originalEn, approvedEn, words,
            alignedMaterialId, schema ou schemaVersion.
            Não adicione, remova, una, divida ou reordene cues.
            Todas as cues devem ter tradução não vazia.
            Entregue um único JSON válido, sem Markdown nem comentários.

            CONTRATO A DEVOLVER:
            """ + codec.toJson(base);
    }

    private static void add(ZipOutputStream output, Path source, String entryName) throws Exception {
        output.putNextEntry(new ZipEntry(entryName));
        try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(source))) {
            input.transferTo(output);
        }
        output.closeEntry();
    }

    private static String readJsonFromZip(Path zipPath) throws Exception {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            String found = null;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".json")) continue;
                if (found != null) {
                    throw new IllegalArgumentException("O ZIP externo contém mais de um JSON.");
                }
                try (var input = zip.getInputStream(entry)) {
                    found = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            if (found == null) throw new IllegalArgumentException("O ZIP externo não contém JSON.");
            return found;
        }
    }
}
