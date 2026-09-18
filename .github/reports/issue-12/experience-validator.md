# Experience Validator Report — Issue #12

## Verdict
FAIL — return to Timing Editor Agent.

## Scope
Desktop Runtime + Source + Wave on branch `feat/java-desktop-restart`.

## Findings

| Requirement | Result | Evidence | Owner | Severity | Correction |
|---|---|---|---|---|---|
| Single Stage/Scene | PASS | GeneratorDesktopApp creates one primary Stage and navigation swaps center content | Desktop Runtime | — | none |
| Source invalid URL feedback | PASS | SourceView blocks advance and exposes product-facing error text | Source Agent | — | none |
| Cache miss/hit feedback | PASS | SourceView distinguishes prepared vs reused content | Source Agent | — | none |
| Source → Wave same window | PASS | NavigationController swaps AppShell center | Desktop Runtime | — | none |
| Video + waveform together | PASS | WaveView workspace renders MediaView and WaveformPane side-by-side | Timing Editor | — | none |
| Seek/playhead | PASS | Waveform seek drives MediaPlayer and MediaPlayer currentTime drives playhead | Timing Editor | — | none |
| Direct IN/OUT drag | PASS | WaveformPane supports marker drag with strict ordering | Timing Editor | — | none |
| Normal zoom 1×–8× | PASS | slider and viewport support 1..8 | Timing Editor | — | none |
| Super Zoom up to 64× | FAIL | WaveViewport clamps at 8× and UI slider max is 8 | Timing Editor | HIGH | add explicit Super Zoom mode up to 64× |
| Pan under zoom | PASS | scroll pans viewport while zoomed | Timing Editor | — | none |
| 1/10/100 ms precision | PASS | Alt/normal/Shift arrow nudges map to 1/10/100 ms | Timing Editor | — | none |
| Selection/full playback | PASS | separate contextual playback commands exist | Timing Editor | — | none |
| Undo/redo timing edits | FAIL | no timing history or undo/redo command | Timing Editor | HIGH | add undo/redo and keyboard shortcuts |
| Safe autosave/equivalent | FAIL | unsaved selection is reconstructed from saved MediaCut only; leaving Wave before rendering can lose timing | Timing Editor | HIGH | persist timing draft automatically per source |
| Visible save state | FAIL | status reports operations but does not expose persistent timing save state | Timing Editor | MEDIUM | add explicit autosave state text |
| Explicit processing/error/success | PASS | Source and Wave provide textual statuses | Source/Timing | — | none |
| Keyboard focus visible | FAIL | stylesheet has no explicit focused treatment for primary edit controls | Timing Editor/Desktop shell | MEDIUM | add visible focus ring for controls |
| Original Source Cache immutable | PASS | FfmpegMediaProcessor writes derived media to workspace and rejects same source/output | Timing Editor | — | none |
| Cue/word diagnostics | N/A | not part of Source/Cut stage | Later timing agents | — | do not invent here |
| Word-boundary snap | N/A | no ASR/word boundaries exist at this stage | Later timing agents | — | do not invent here |

## Required return
Timing Editor Agent must address all FAIL items and provide tests for:
- 64× viewport behavior;
- undo/redo history;
- autosaved draft restore;
- invalid/corrupt draft safety;
- save-state transition where practical.

After correction, return to Orchestrator for a second Experience Validator pass.
