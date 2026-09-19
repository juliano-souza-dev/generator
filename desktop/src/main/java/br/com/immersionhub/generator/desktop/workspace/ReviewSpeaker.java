package br.com.immersionhub.generator.desktop.workspace;

import java.util.Objects;

public record ReviewSpeaker(String id, String name) {
    public ReviewSpeaker {
        id = require(id, "id");
        name = require(name, "name");
    }

    private static String require(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " não pode ser vazio.");
        return normalized;
    }
}
