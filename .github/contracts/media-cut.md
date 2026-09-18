# Contract — MediaCut

Owner: Orchestrator

Producer: Timing Editor Agent

## Purpose
Represent the approved scene cut derived from an immutable cached source.

## Fields
- `sourceId: String`
- `sourcePath: Path`
- `outputPath: Path`
- `startMs: long`
- `endMs: long`
- `durationMs: long`
- `createdAt: Instant`

## Invariants
1. `0 <= startMs < endMs <= SourceMedia.durationMs`.
2. `durationMs == endMs - startMs`.
3. `sourcePath` is the original cached source and is never modified.
4. `outputPath` is a distinct derived media file.
5. `outputPath` exists before the Timing Agent reports success.
6. downstream agents consume `MediaCut`, not raw UI state.

Any contract change requires Orchestrator approval.
