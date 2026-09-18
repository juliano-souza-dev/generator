package br.com.immersionhub.generator.desktop.timing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class TimingHistory {
    private final Deque<TimingRange> undo = new ArrayDeque<>();
    private final Deque<TimingRange> redo = new ArrayDeque<>();
    private TimingRange current;

    public TimingHistory(long startMs, long endMs) {
        current = new TimingRange(startMs, endMs);
    }

    public TimingRange current() {
        return current;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public void record(long startMs, long endMs) {
        TimingRange next = new TimingRange(startMs, endMs);
        if (next.equals(current)) return;
        undo.push(current);
        current = next;
        redo.clear();
    }

    public Optional<TimingRange> undo() {
        if (undo.isEmpty()) return Optional.empty();
        redo.push(current);
        current = undo.pop();
        return Optional.of(current);
    }

    public Optional<TimingRange> redo() {
        if (redo.isEmpty()) return Optional.empty();
        undo.push(current);
        current = redo.pop();
        return Optional.of(current);
    }
}
