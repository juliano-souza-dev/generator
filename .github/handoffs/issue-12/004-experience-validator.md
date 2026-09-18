# Handoff 004 — Issue #12 → Experience Validator

## Status
READY — Timing Editor technical delivery accepted by Orchestrator.

## Issue
#12 — Converter o Generator em aplicativo desktop instalável

## Branch
`feat/java-desktop-restart`

## Agent owner
Experience Validator

## Role
Validate product experience. Do not implement fixes.

## Accepted upstream inputs
- Desktop Runtime Agent: accepted.
- Source Ingestion & Cache Agent: accepted.
- Timing Editor Agent: technically accepted.
- SourceMedia contract: frozen.
- MediaCut contract: technically validated.

## Product context supplied by Orchestrator

### Desktop foundation
- one Stage;
- one Scene;
- normal navigation replaces central content only;
- no WebView/browser in the main flow;
- desktop-first;
- keyboard-usable;
- focus must remain visible;
- no frequent-edit modal.

### Subtitle Edit reference for Source → Wave
Keep relevant media/timing information in one context.
For the Source/Cut stage, applicable requirements are:
- video and waveform visible together;
- direct IN/OUT dragging;
- immediate playhead movement;
- horizontal zoom and pan;
- selection playback;
- 1/10/100 ms precision adjustments;
- keyboard-first operations;
- undo/redo for timing edits;
- safe autosave or equivalent protection against accidental loss;
- visible save state;
- explicit processing/error/success states;
- main task visible without hunting across multiple areas.

### Global Wave product requirement
- normal Zoom: 1×–8×;
- Super Zoom: up to 64×;
- pan under zoom;
- zoom anchored around relevant playhead/context;
- efficient redraw;
- contextual playback;
- touch support where applicable;
- nudge 1/10/100 ms.

### Not applicable yet in Source/Cut
These belong to cue/word timing phases and must NOT be invented here:
- active subtitle cue text;
- word-boundary snap;
- cue readability diagnostics;
- CTC confidence queue;
- approve/problem navigation.

## Current implementation to inspect
- `SourceView`
- `WaveView`
- `WaveformPane`
- `WaveViewport`
- `TimingSelection`
- `FfmpegMediaProcessor`
- `NavigationController`
- `desktop.css`

## Mandatory scenarios
1. first opening;
2. invalid Source URL;
3. Source cache miss;
4. Source cache hit;
5. Source → Wave;
6. Wave → Source;
7. same Stage/Scene throughout;
8. video + waveform context;
9. seek;
10. mark IN;
11. mark OUT;
12. drag IN/OUT;
13. normal zoom 1×–8×;
14. Super Zoom up to 64×;
15. pan;
16. 1 ms nudge;
17. 10 ms nudge;
18. 100 ms nudge;
19. selection playback;
20. full-source playback;
21. save cut;
22. leave/re-enter Wave without silent timing loss;
23. undo/redo timing edits;
24. processing/error/success visibility;
25. keyboard focus visibility.

## Output format
For every finding:
- requirement;
- PASS / FAIL / N/A;
- evidence;
- owner agent;
- severity;
- exact correction required.

## Gate
QA remains BLOCKED unless Orchestrator accepts a Validator PASS.
