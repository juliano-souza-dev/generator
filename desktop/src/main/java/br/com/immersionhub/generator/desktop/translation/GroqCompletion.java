package br.com.immersionhub.generator.desktop.translation;

public record GroqCompletion(
    String content,
    GroqRateSnapshot rate
) {}
