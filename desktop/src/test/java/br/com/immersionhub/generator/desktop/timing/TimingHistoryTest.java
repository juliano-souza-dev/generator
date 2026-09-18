package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimingHistoryTest {
    @Test
    void undoAndRedoRestoreTimingEdits() {
        TimingHistory history = new TimingHistory(0, 10_000);

        history.record(500, 10_000);
        history.record(500, 9_000);

        assertTrue(history.canUndo());
        assertEquals(new TimingRange(500, 10_000), history.undo().orElseThrow());
        assertEquals(new TimingRange(0, 10_000), history.undo().orElseThrow());
        assertFalse(history.canUndo());

        assertEquals(new TimingRange(500, 10_000), history.redo().orElseThrow());
        assertEquals(new TimingRange(500, 9_000), history.redo().orElseThrow());
        assertFalse(history.canRedo());
    }

    @Test
    void newEditClearsRedoHistory() {
        TimingHistory history = new TimingHistory(0, 10_000);
        history.record(100, 10_000);
        history.undo();
        history.record(200, 10_000);
        assertFalse(history.canRedo());
    }
}
