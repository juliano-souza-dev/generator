package br.com.immersionhub.generator.desktop.editorial;

import java.util.Objects;

public record WordReviewMutation(
    EditorialMaterial material,
    WordReviewPosition position
) {
    public WordReviewMutation {
        material = Objects.requireNonNull(material, "material");
        position = Objects.requireNonNull(position, "position");
    }
}
