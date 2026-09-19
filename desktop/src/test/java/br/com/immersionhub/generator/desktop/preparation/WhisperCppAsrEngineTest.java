package br.com.immersionhub.generator.desktop.preparation;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperCppAsrEngineTest {
    @Test
    void parsesMillisecondOffsetsFromWhisperJson() throws Exception {
        Path dir = Files.createTempDirectory("whisper-json");
        Path model = Files.writeString(dir.resolve("model.bin"), "model");
        Path json = dir.resolve("result.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets": {"from": 1240, "to": 2860},
                  "text": "What are you gonna do?",
                  "tokens": [
                    {"text": "What", "offsets": {"from": 1240, "to": 1510}},
                    {"text": "are", "offsets": {"from": 1510, "to": 1660}},
                    {"text": "[_TT_143]", "offsets": {"from": 2860, "to": 2870}}
                  ]
                }
              ]
            }
            """);

        WhisperCppAsrEngine engine = new WhisperCppAsrEngine(() -> dir.resolve("unused.exe"), model, "test");
        AsrResult result = engine.parse(json);

        assertEquals("What are you gonna do?", result.text());
        assertEquals(1240, result.segments().getFirst().startMs());
        assertEquals(2860, result.segments().getFirst().endMs());
        assertEquals(1510, result.words().getFirst().endMs());
        assertEquals(2, result.words().size());
        assertTrue(result.words().stream().noneMatch(word -> word.text().startsWith("[_")));
    }
}
