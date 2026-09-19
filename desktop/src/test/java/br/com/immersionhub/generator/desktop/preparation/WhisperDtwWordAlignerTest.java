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
    void clampsResidualDtwOvershootAtRealCutBoundary() throws Exception {
        Path dir = Files.createTempDirectory("dtw-boundary");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets":{"from":132120,"to":134200},
                  "text":" final sign.",
                  "tokens":[
                    {"text":" final","t_dtw":13250},
                    {"text":" sign","t_dtw":13380},
                    {"text":".","t_dtw":13400}
                  ]
                }
              ]
            }
            """);

        List<TimedText> result = aligner(dir).parseDtwWords(json, 134182);

        assertEquals(134182, result.getLast().endMs());
        assertTrue(result.getFirst().startMs() >= 132120);
        assertDoesNotThrow(() -> AlignedMaterial.validate(result, 134182));
    }

    @Test
    void distributesDuplicateAnchorRunWithoutOverlap() throws Exception {
        Path dir = Files.createTempDirectory("dtw-duplicate-run");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets":{"from":0,"to":1000},
                  "text":" one two three four",
                  "tokens":[
                    {"text":" one","t_dtw":10},
                    {"text":" two","t_dtw":40},
                    {"text":" three","t_dtw":40},
                    {"text":" four","t_dtw":80}
                  ]
                }
              ]
            }
            """);

        List<TimedText> result = aligner(dir).parseDtwWords(json, 1000);

        assertEquals(4, result.size());
        assertEquals("one", result.get(0).text());
        assertEquals("two", result.get(1).text());
        assertEquals("three", result.get(2).text());
        assertEquals("four", result.get(3).text());
        assertTrue(result.get(1).endMs() <= result.get(2).startMs());
        assertTrue(result.stream().allMatch(word -> word.endMs() > word.startMs()));
        assertDoesNotThrow(() -> AlignedMaterial.validate(result, 1000));
    }

    @Test
    void distributesThreeWordsSharingAnchorAtSegmentStart() throws Exception {
        Path dir = Files.createTempDirectory("dtw-duplicate-start");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets":{"from":100,"to":900},
                  "text":" alpha beta gamma delta",
                  "tokens":[
                    {"text":" alpha","t_dtw":20},
                    {"text":" beta","t_dtw":20},
                    {"text":" gamma","t_dtw":20},
                    {"text":" delta","t_dtw":70}
                  ]
                }
              ]
            }
            """);

        List<TimedText> result = aligner(dir).parseDtwWords(json, 1000);

        assertEquals(100, result.getFirst().startMs());
        assertEquals(4, result.size());
        assertTrue(result.get(0).endMs() <= result.get(1).startMs());
        assertTrue(result.get(1).endMs() <= result.get(2).startMs());
        assertDoesNotThrow(() -> AlignedMaterial.validate(result, 1000));
    }

    @Test
    void distributesDuplicateAnchorRunAtSegmentEnd() throws Exception {
        Path dir = Files.createTempDirectory("dtw-duplicate-end");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets":{"from":0,"to":1000},
                  "text":" alpha beta gamma",
                  "tokens":[
                    {"text":" alpha","t_dtw":20},
                    {"text":" beta","t_dtw":80},
                    {"text":" gamma","t_dtw":80}
                  ]
                }
              ]
            }
            """);

        List<TimedText> result = aligner(dir).parseDtwWords(json, 1000);

        assertEquals(1000, result.getLast().endMs());
        assertTrue(result.get(1).endMs() <= result.get(2).startMs());
        assertDoesNotThrow(() -> AlignedMaterial.validate(result, 1000));
    }

    @Test
    void rejectsTrulyRegressiveDtwAnchors() throws Exception {
        Path dir = Files.createTempDirectory("dtw-regressive");
        Path json = dir.resolve("aligned.json");
        Files.writeString(json, """
            {
              "transcription": [
                {
                  "offsets":{"from":0,"to":1000},
                  "text":" one two three",
                  "tokens":[
                    {"text":" one","t_dtw":20},
                    {"text":" two","t_dtw":60},
                    {"text":" three","t_dtw":40}
                  ]
                }
              ]
            }
            """);

        IOException error = assertThrows(IOException.class, () -> aligner(dir).parseDtwWords(json, 1000));
        assertTrue(error.getMessage().contains("regressivas"));
        assertTrue(error.getMessage().contains("word[2]"));
    }

    @Test
    void rejectsMaterialDtwOvershootAndReportsSegment() throws Exception {
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

        IOException error = assertThrows(IOException.class, () -> aligner(dir).parseDtwWords(json, 1000));
        assertTrue(error.getMessage().contains("segment[0]"));
        assertTrue(error.getMessage().contains("endMs=1200"));
        assertTrue(error.getMessage().contains("durationMs=1000"));
    }
}
