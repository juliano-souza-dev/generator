package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveViewportTest {
    @Test
    void mapsCoordinatesAtOneX() {
        WaveViewport viewport = new WaveViewport(60_000);
        assertEquals(0, viewport.xToMs(0, 600));
        assertEquals(30_000, viewport.xToMs(300, 600));
        assertEquals(60_000, viewport.xToMs(600, 600));
        assertEquals(300.0, viewport.msToX(30_000, 600), 0.001);
    }

    @Test
    void supportsSuperZoomUpToSixtyFourX() {
        WaveViewport viewport = new WaveViewport(64_000);
        viewport.setZoom(64.0, 32_000);
        assertEquals(64.0, viewport.zoom(), 0.001);
        assertEquals(1_000, viewport.visibleDurationMs());

        viewport.setZoom(200.0, 32_000);
        assertEquals(64.0, viewport.zoom(), 0.001);
    }

    @Test
    void mapsCoordinatesInsideZoomedViewport() {
        WaveViewport viewport = new WaveViewport(60_000);
        viewport.setZoom(2.0, 30_000);
        assertEquals(15_000, viewport.viewStartMs());
        assertEquals(30_000, viewport.visibleDurationMs());
        assertEquals(15_000, viewport.xToMs(0, 600));
        assertEquals(30_000, viewport.xToMs(300, 600));
        assertEquals(45_000, viewport.xToMs(600, 600));
    }

    @Test
    void panStaysInsideMediaBounds() {
        WaveViewport viewport = new WaveViewport(60_000);
        viewport.setZoom(4.0, 30_000);

        viewport.panByFraction(-100);
        assertEquals(0, viewport.viewStartMs());

        viewport.panByFraction(100);
        assertEquals(45_000, viewport.viewStartMs());
        assertEquals(60_000, viewport.viewEndMs());
    }

    @Test
    void boundariesNeverCross() {
        WaveViewport viewport = new WaveViewport(60_000);
        assertEquals(19_999, viewport.clampIn(30_000, 20_000));
        assertEquals(20_001, viewport.clampOut(10_000, 20_000));
        assertEquals(60_000, viewport.clampOut(80_000, 20_000));
    }

    @Test
    void keepVisibleClampsPlayheadAndViewport() {
        WaveViewport viewport = new WaveViewport(60_000);
        viewport.setZoom(4.0, 0);
        viewport.keepVisible(59_000);
        assertTrue(viewport.viewStartMs() >= 44_000);
        assertTrue(viewport.viewEndMs() <= 60_000);

        viewport.keepVisible(-10_000);
        assertEquals(0, viewport.viewStartMs());
    }
}
