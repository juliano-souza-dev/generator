# Handoff 002 — Issue #16 → Experience Validator

## Status
PASS

## Input reviewed
Source Ingestion & Cache Agent delivery on PR #19.

## Product requirement
The user observed that preparing the same YouTube source could fail repeatedly and later succeed without URL changes.

Expected experience:
- one user action should absorb ordinary transient failures;
- no repeated manual clicking should be required for transient conditions;
- valid cache hit must stay fast and must not redownload;
- UI must not expose yt-dlp/FFmpeg internals;
- Wave/Timing behavior must remain untouched.

## Validation

### One-action retry
PASS
- transient failures are retried internally up to a bounded maximum;
- small deterministic backoff is applied.

### Definitive failures
PASS
- clearly unavailable/private/invalid sources do not enter useless retry loops.

### Format resilience
PASS
- ordered format strategies are attempted;
- FFmpeg location is explicitly supplied;
- partial artifacts are cleaned before fallback.

### Cache
PASS
- SourceAcquisitionService checks cache before inspect/download;
- local harness proved second acquisition of the canonical URL does not inspect/download again.

### Product language
PASS after correction
- invalid URL keeps a specific product message;
- technical/transient failures show: “Não foi possível preparar essa fonte agora. Tente novamente.”;
- yt-dlp output is retained only in local technical diagnostics.

### Scope safety
PASS
- SourceMedia unchanged;
- Wave/Timing untouched;
- no M2 implementation included.

## Evidence
- JDK 21 compile PASS;
- deterministic retry/fallback harness PASS;
- cache harness PASS;
- regression tests added in PR #19;
- GitHub Actions did not start for the PR head, so CI PASS is not claimed.

## Result
**Experience Validator: PASS**

Final functional verification remains with User Acceptance QA on the updated Windows installer.
