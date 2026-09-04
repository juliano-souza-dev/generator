# Alpha 1.36 — Music Shadowing Reuse

## Scope
- Music now follows `WbW Timing → Shadowing → Materials`.
- The Shadowing editor, waveform, pause markers, preview, plan builder and `shadowing.json` are the exact same implementation already used by Scene.
- Music still skips Connected Speech.
- Materials are blocked until Music Shadowing is finalized.
- Final HUB JSON now carries the same `shadowingConfig` + `shadowingPractice` for Music.
- Alpha 1.35 External AI contract synchronization remains unchanged.

## No new Shadowing engine
This release intentionally introduces no second Music-specific Shadowing schema, editor or renderer.
