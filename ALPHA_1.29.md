# Alpha 1.29 — WbW expands cue bounds

## Behavior

The reviewed WbW timeline can define the audible edge of a cue. If a word starts before `speech_start_ms` or ends after `speech_end_ms`, the cue expands to contain the word. The word timing is never clamped to fit the old cue.

Example fixed in this release:

- cue 1: `3150–4170 ms`
- `go.`: `3618–4411 ms`
- persisted/exported cue: `3150–4411 ms`
- `go.` remains `3618–4411 ms`

The same normalization migrates existing WbW documents and is applied defensively when building `hub_final.json`.
