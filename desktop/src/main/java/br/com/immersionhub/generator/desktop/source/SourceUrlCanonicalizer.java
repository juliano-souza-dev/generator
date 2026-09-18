package br.com.immersionhub.generator.desktop.source;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class SourceUrlCanonicalizer {
    public String canonicalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Informe uma URL do YouTube.");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("A URL informada não é válida.", exception);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Use uma URL HTTP ou HTTPS.");
        }

        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) host = host.substring(4);
        if (host.startsWith("m.")) host = host.substring(2);

        String videoId = null;
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (host.equals("youtu.be")) {
            videoId = firstPathSegment(path);
        } else if (host.equals("youtube.com") || host.endsWith(".youtube.com")) {
            if (path.equals("/watch")) {
                videoId = query(uri).get("v");
            } else if (path.startsWith("/shorts/") || path.startsWith("/embed/") || path.startsWith("/live/")) {
                videoId = segmentAfterPrefix(path);
            }
        }

        if (videoId == null || !videoId.matches("[A-Za-z0-9_-]{6,64}")) {
            throw new IllegalArgumentException("Informe o link de um vídeo válido do YouTube.");
        }

        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private static String firstPathSegment(String path) {
        return Arrays.stream(path.split("/"))
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse(null);
    }

    private static String segmentAfterPrefix(String path) {
        String[] parts = path.split("/");
        return parts.length >= 3 ? parts[2] : null;
    }

    private static Map<String, String> query(URI uri) {
        if (uri.getRawQuery() == null || uri.getRawQuery().isBlank()) return Map.of();
        return Arrays.stream(uri.getRawQuery().split("&"))
            .map(part -> part.split("=", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> decode(parts[0]),
                parts -> decode(parts[1]),
                (left, right) -> left
            ));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
