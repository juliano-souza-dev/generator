package br.com.immersionhub.generator.desktop.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimecodeTest {
    @Test
    void parsesAndFormatsMilliseconds() {
        assertEquals("00:01:02.345", Timecode.format(62_345));
        assertEquals(62_345, Timecode.parse("00:01:02.345"));
        assertEquals(62_500, Timecode.parse("1:02.5"));
    }
}
