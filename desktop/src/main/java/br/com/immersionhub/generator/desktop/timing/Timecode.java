package br.com.immersionhub.generator.desktop.timing;

import java.util.Locale;

public final class Timecode {
    private Timecode() {}

    public static String format(long ms) {
        long safe = Math.max(0, ms);
        long hours = safe / 3_600_000;
        long minutes = (safe % 3_600_000) / 60_000;
        long seconds = (safe % 60_000) / 1_000;
        long millis = safe % 1_000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    public static long parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Timecode vazio.");
        String[] parts = value.trim().split(":");
        try {
            if (parts.length == 1) {
                return Math.round(Double.parseDouble(parts[0].replace(',', '.')) * 1000.0);
            }
            if (parts.length == 2) {
                long minutes = Long.parseLong(parts[0]);
                double seconds = Double.parseDouble(parts[1].replace(',', '.'));
                return minutes * 60_000 + Math.round(seconds * 1000.0);
            }
            if (parts.length == 3) {
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                double seconds = Double.parseDouble(parts[2].replace(',', '.'));
                return hours * 3_600_000 + minutes * 60_000 + Math.round(seconds * 1000.0);
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Timecode inválido.", exception);
        }
        throw new IllegalArgumentException("Use HH:MM:SS.mmm.");
    }
}
