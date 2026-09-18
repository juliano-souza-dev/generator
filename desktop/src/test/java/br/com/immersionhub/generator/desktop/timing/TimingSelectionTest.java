package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimingSelectionTest {
    @Test
    void keepsRangeInsideMediaAndPreservesStrictOrdering() {
        TimingSelection selection = new TimingSelection(60_000);
        selection.setRange(10_000, 20_000);

        selection.nudge(Boundary.IN, 20_000);
        assertEquals(19_999, selection.startMs());

        selection.nudge(Boundary.OUT, -20_000);
        assertEquals(20_000, selection.endMs());

        selection.setStartMs(-500);
        selection.setEndMs(80_000);
        assertEquals(0, selection.startMs());
        assertEquals(60_000, selection.endMs());
    }

    @Test
    void rejectsInvalidExplicitRange() {
        TimingSelection selection = new TimingSelection(10_000);
        assertThrows(IllegalArgumentException.class, () -> selection.setRange(5_000, 5_000));
        assertThrows(IllegalArgumentException.class, () -> selection.setRange(0, 11_000));
    }
}
