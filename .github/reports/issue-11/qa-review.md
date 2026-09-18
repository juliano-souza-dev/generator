# QA / Review Report — Issue #11

## Verdict
PASS — Milestone 2 regression gate accepted.

## Runtime under review
Active runtime only: Java 21 + JavaFX + Maven. Python is legacy/reference and is not an active release gate.

## Accepted upstream evidence
- Issue #10 completed after Timing Editor correction and Experience Validator PASS.
- Human QA confirmed install, media acquisition, cache reuse and recut behavior.
- Linux Windows crossbuild run 35396161375: SUCCESS for Java build/tests, Windows app assembly, installer generation and artifact upload.

## Final native Windows evidence
Candidate workflow commit: 648ca09124e73b7bd2a53792b78629449c03e835
Run: 35397822953
Job: windows-installer — SUCCESS

Validated gates:
- JDK 21 setup: PASS
- Java tests and package: PASS
- self-contained app image: PASS
- bundled runtime/media pipeline smoke: PASS
- Inno Setup provisioning: PASS
- Windows installer build: PASS
- real installer install + installed launcher smoke: PASS
- installed timing smoke: PASS
- registered uninstaller execution: PASS
- persistent user-data preservation: PASS
- installer artifact upload: PASS

Artifact:
- ImmersionHub-Generator-Java-Windows
- artifact id 10569870637
- sha256 b8979decfeee5a3d1e7740125b6294285243d106e2baa15238f11b3fc150b013

## Regression findings converted into gates
QA exposed a malformed/duplicated PowerShell section in the Windows installer workflow and an unsafe parser for the registered uninstall command. The workflow itself is the automated regression gate for this infrastructure defect: malformed YAML cannot create jobs, and the native lifecycle step must complete successfully before artifact publication.

Final correction removes the duplicated corrupt tail and parses quoted registered uninstall commands deterministically without the fragile regex path.

## Scope adaptation
The original issue text predates the Java desktop migration. Python-suite/mobile assumptions are therefore not treated as active M2 failures. The relevant M2 release surface is the Java desktop Source → Wave flow and its installer/lifecycle contracts.

## Final recommendation
QA Review Agent: PASS.
PR #23 may leave draft, be merged to main, and Issue #11 may be closed as completed after the report commit itself is integrated.
