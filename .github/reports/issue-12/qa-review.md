# QA / Review Report — Issue #12

## First-pass verdict
FAIL — return installer lifecycle to Desktop Runtime Agent.

## Accepted upstream gates
- Desktop Runtime architecture: accepted.
- Source Ingestion & Cache: accepted.
- Timing Editor: accepted after corrections.
- Experience Validator: PASS.

## Automated evidence
The corrected Java build passes:
- Maven compilation/tests;
- app-image creation in prior/current runs;
- packaged source/timing smoke tests in prior/current runs.

## Blocking finding

### QA-INSTALL-01 — Windows Setup lifecycle lacks deterministic PASS
Multiple workflow runs reach `Install and smoke test generated Setup` without producing a clean final PASS. Earlier candidate runs were cancelled while inside this combined install/smoke/uninstall step.

The current workflow combines:
1. EXE install;
2. installed launcher smoke;
3. installed timing smoke;
4. uninstall;
5. removal assertion

inside one opaque step with no bounded wait or phase-level diagnostics.

This is not acceptable as the final release gate.

## Required correction
Desktop Runtime Agent must:
- keep the EXE as the real deliverable;
- test the real EXE installer;
- bound installer/uninstaller waits;
- print phase-level diagnostics;
- locate the registered uninstall command instead of assuming that rerunning the installer with `/uninstall` is always sufficient;
- preserve user data outside the installation directory;
- add workflow concurrency so stale PR builds are cancelled and only the newest candidate consumes the final gate.

## Re-run criteria
QA will PASS only when the candidate head shows:
- Maven PASS;
- app-image PASS;
- packaged source/timing smoke PASS;
- EXE installer build PASS;
- real EXE silent install PASS;
- installed launcher smoke PASS;
- installed timing smoke PASS;
- uninstall PASS;
- installed binary removed;
- persistent data directory contract remains valid;
- artifact upload PASS.

Issue #12 remains open.
