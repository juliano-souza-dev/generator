# Experience Validator Report — Issue #12

## Second-pass verdict
PASS — product experience gate accepted.

## Scope
Desktop Runtime + Source + Wave on branch `feat/java-desktop-restart`.

## Evidence matrix

| Requirement | Result | Evidence |
|---|---|---|
| Single Stage/Scene | PASS | GeneratorDesktopApp creates one primary Stage and navigation swaps AppShell center |
| Source invalid URL feedback | PASS | SourceView blocks advance and exposes product-facing error text |
| Cache miss/hit feedback | PASS | SourceView distinguishes prepared vs reused content |
| Source → Wave same window | PASS | NavigationController swaps center content |
| Video + waveform together | PASS | WaveView renders MediaView and WaveformPane side-by-side |
| Seek/playhead synchronization | PASS | Waveform seek drives MediaPlayer; MediaPlayer currentTime drives playhead |
| Direct IN/OUT drag | PASS | WaveformPane drags boundaries with strict clamping |
| Normal zoom 1×–8× | PASS | default slider range is 1..8 |
| Super Zoom up to 64× | PASS | explicit Super Zoom mode extends slider and WaveViewport to 64× |
| Pan under zoom | PASS | scroll pans WaveViewport while zoomed |
| 1/10/100 ms precision | PASS | Alt/normal/Shift arrows map to 1/10/100 ms |
| Selection/full playback | PASS | contextual selection and full-source playback are separate |
| Undo/redo | PASS | TimingHistory + Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z |
| Safe timing autosave | PASS | TimingDraftRepository atomically saves per-source draft on committed edits and exit |
| Leave/re-enter Wave | PASS | per-source draft is restored before last rendered MediaCut |
| Visible save state | PASS | saveStateLabel communicates recovered/autosaved/rendered state textually |
| Explicit processing/error/success | PASS | Source and Wave provide textual states |
| Keyboard focus visible | PASS | focused edit/navigation controls and waveform have explicit focus border |
| Keyboard save | PASS | Ctrl+S invokes cut save |
| Original Source Cache immutable | PASS | derived cut is written to workspace; source/output equality is rejected |
| Cue/word diagnostics | N/A | later cue/word timing stage |
| Word-boundary snap | N/A | no ASR/word boundaries exist at Source/Cut stage |

## Regression evidence
- new `TimingHistoryTest`;
- new `TimingDraftRepositoryTest`;
- expanded `WaveViewportTest` including 64×;
- existing TimingSelection, MediaCut and waveform reduction tests remain part of Maven gate;
- current corrected build passed Maven compilation/tests.

## Result
Experience Validator returns **PASS** to Orchestrator.

QA/Review may now be released for the final issue #12 gate.
