# Contract — PreparedMaterial / AlignedMaterial

Owner: Orchestrator

Producers:
- ASR Agent produces `PreparedMaterial`
- Word Alignment Agent produces `AlignedMaterial`

Consumer:
- next approved translation stage (#18)

## Input identity
The preparation identity is derived from:
- `MediaCut.sourceId`
- `MediaCut.startMs`
- `MediaCut.endMs`
- preparation pipeline version
- ASR model/runtime version

The alignment identity additionally includes the word-alignment version.

## PreparedMaterial
Contains:
- immutable reference to the approved `MediaCut`;
- local technical audio, mono 16 kHz PCM;
- full English transcription;
- timed ASR segments;
- token timing/probability when available;
- creation timestamp and version metadata.

## AlignedMaterial
Contains:
- reference to the accepted `PreparedMaterial`;
- refined English words with `startMs` / `endMs`;
- probability/confidence when available;
- alignment version and deterministic identity.

`AlignedMaterial.transcription()` is the canonical handoff to #18: it preserves the accepted English text and segments while replacing preliminary token timing with refined word timing.

## Temporal invariants
1. `0 <= startMs < endMs <= MediaCut.durationMs`.
2. Word order is monotonic and words never overlap.
3. Alignment must not change the accepted transcript text.
4. No timing can escape the approved cut timeline.

## Storage and reuse
- all artifacts are stored under persistent application data, outside the installation directory;
- source media and approved cut are never overwritten;
- technical audio and snapshots are derived artifacts;
- a matching identity may be reused without executing FFmpeg/ASR/alignment again;
- incomplete or failed processing is never published as a valid snapshot.

## UI boundary
Normal UI exposes product states only, such as:
- Preparando material…
- Ajustando tempos…
- Material pronto.
- Não foi possível preparar o material.

Technology/model/process details belong only in technical logs.

Any change to this contract requires Orchestrator approval.
