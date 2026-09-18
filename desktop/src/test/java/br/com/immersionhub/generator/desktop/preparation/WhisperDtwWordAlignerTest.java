package br.com.immersionhub.generator.desktop.preparation;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WhisperDtwWordAlignerTest {
    private WhisperDtwWordAligner aligner(Path dir) throws Exception {
        Path model = Files.writeString(dir.resolve("model.bin"), "model");
        return new WhisperDtwWordAligner(() -> dir.resolve("unused.exe"), model, "base.en", "dtw-test");
    }

    @Test
    void disablesFlashAttentionWheneverDtwAlignmentIsRequested() {
        List<String> command = WhisperDtwWordAligner.command(
            Path.of("whisper-cli.exe"),
            Path.of("model.bin"),
            Path.of("audio.wav"),
            Path.of("result"),
            "base.en"
        );

        assertTrue(command.contains("-dtw"));
        assertTrue(command.contains("-nfa"));
        assertTrue(command.indexOf("-nfa") > command.indexOf("-dtw"));
    }

    @Test
    void buildsWordBoundariesFromDtwTokenAnchorsAndPreservesAcceptedTranscript() throws Exception {
        Path dir = Files.createTempDirectory("dtw-json");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets":{"from":100,"to":700},
                  "text":" What are you?",
                  "tokens":[
                    {"text":"[_BEG_]","t_dtw":0},
                    {"text":" What","t_dtw":15},
                    {"text":" are","t_dtw":32},
                    {"text":" you","t_dtw":51},
                    {"text":"?","t_dtw":56},
                    {"text":"[_TT_070]","t_dtw":70}
                  ]
                }
              ]
            }
            """);

        WhisperDtwWordAligner aligner = aligner(dir);
        List<TimedText> candidates = aligner.parseDtwWords(json, 1000);
        List<TimedText> result = aligner.preserveTranscript("What are you?", candidates);

        assertEquals(List.of("What", "are", "you?"), result.stream().map(TimedText::text).toList());
        assertEquals(100, result.getFirst().startMs());
        assertEquals(700, result.getLast().endMs());
        assertTrue(result.get(0).endMs() <= result.get(1).startMs());
        assertTrue(result.get(1).endMs() <= result.get(2).startMs());
    }

    @Test
    void rejectsAlignmentThatChangesTranscript() throws Exception {
        Path dir = Files.createTempDirectory("dtw-mismatch");
        WhisperDtwWordAligner aligner = aligner(dir);
        assertThrows(IOException.class, () -> aligner.preserveTranscript(
            "What are you?",
            List.of(new TimedText("Where", 0, 100), new TimedText("are", 100, 200), new TimedText("you?", 200, 300))
        ));
    }

    @Test
    void rejectsDtwWordOutsideCutDuration() throws Exception {
        Path dir = Files.createTempDirectory("dtw-range");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets":{"from":0,"to":1200},
                  "text":" hello",
                  "tokens":[{"text":" hello","t_dtw":10}]
                }
              ]
            }
            """);
        assertThrows(IOException.class, () -> aligner(dir).parseDtwWords(json, 1000));
    }
}
