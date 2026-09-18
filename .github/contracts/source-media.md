# Contract — SourceMedia

Owner: Orchestrator

Consumer: Timing Editor Agent

Producer: Source Ingestion & Cache Agent

## Purpose
Represent a source that is already local, valid and ready for media processing.

## Fields
- `sourceId: String` — deterministic source identity.
- `canonicalUrl: String` — normalized origin URL used for cache lookup.
- `localPath: Path` — existing local media file.
- `title: String` — display title.
- `durationMs: long` — source duration in milliseconds.
- `fetchedAt: Instant` — when the local source was acquired.
- `cacheHit: boolean` — true when no new download occurred in the current acquisition.

## Invariants
1. localPath exists and is a regular file.
2. durationMs > 0.
3. canonicalUrl is non-empty.
4. sourceId is stable for the same canonical source.
5. consumers do not redownload from canonicalUrl.
6. consumers do not mutate the cached original.
7. derived/cut media must be written elsewhere.

Changes to this contract require Orchestrator approval before producer or consumer implementation changes.
