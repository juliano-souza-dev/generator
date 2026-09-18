package br.com.immersionhub.generator.desktop.source;

public record SourceDescriptor(
    String remoteId,
    String title,
    long durationMs
) {
    public SourceDescriptor {
        remoteId = remoteId == null ? "" : remoteId.trim();
        title = title == null || title.isBlank() ? "Fonte de vídeo" : title.trim();
        if (durationMs <= 0) {
            throw new IllegalArgumentException("A fonte precisa informar uma duração válida.");
        }
    }
}
