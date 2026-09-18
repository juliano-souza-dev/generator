# Handoff 005 — Issue #12 → QA / Review

## Status
READY — Experience Validator PASS accepted by Orchestrator.

## Objetivo
Executar o gate final da issue #12.

## Gates mínimos
- Maven clean test package;
- app-image autocontida;
- smoke do runtime;
- smoke das ferramentas empacotadas;
- smoke do Source;
- smoke do Timing;
- geração do instalador Windows;
- instalação silenciosa no runner;
- smoke do app instalado;
- desinstalação;
- projetos/cache/settings fora da pasta de instalação;
- contratos SourceMedia/MediaCut preservados;
- nenhuma nova Stage no fluxo normal.

## Critério
Qualquer falha relevante mantém #12 aberta.

## Saída
- PASS/FAIL;
- evidências do commit candidato;
- decisão recomendada ao Orquestrador.


## Candidate context
Experience Validator second pass: PASS.

QA must pay special attention to the Windows installer gate. Earlier workflow runs reached the Setup install/uninstall phase but did not provide a clean final PASS. Treat installer lifecycle as a blocking gate, not as optional evidence.

Current product/domain gates already accepted:
- Desktop Runtime;
- Source Ingestion & Cache;
- Timing Editor technical delivery;
- Experience Validator.
