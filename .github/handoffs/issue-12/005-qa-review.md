# Handoff 005 — Issue #12 → QA / Review

## Status
CHANGES REQUESTED — falta provar o instalador executado, não apenas sua geração.

## QA-01 — Install smoke ausente
Owner: Desktop Runtime Agent

O pipeline atual:
- prova o app-image autocontido;
- gera o instalador EXE;
- publica o artefato.

Mas não:
- executa o instalador;
- confirma a instalação per-user;
- executa o launcher instalado;
- desinstala ao final.

**Correção exigida:** o CI deve instalar silenciosamente o EXE produzido, executar `--smoke-test` no launcher instalado e confirmar saída zero. Depois deve desinstalar silenciosamente.

O QA permanece aberto até essa evidência existir.

## Build candidato
GitHub Actions run `35368116777`.
Branch `feat/java-desktop-restart`.

QA deve validar o head atual completo, não runs intermediários.

## Objetivo
Determinar se a issue #12 cumpre integralmente seus critérios e pode ser fechada como completed.

## Evidências mínimas exigidas
- Maven tests verdes;
- runtime autocontido smoke-tested;
- downloader empacotado e verificado;
- pipeline FFmpeg empacotado e testado;
- waveform real em mídia sintética;
- recorte real em mídia sintética;
- instalador Windows produzido;
- Source Cache preservado;
- contratos SourceMedia/MediaCut válidos;
- nenhuma issue da Milestone 2 iniciada antes desta aprovação.

## Saída
APPROVED ou CHANGES REQUESTED.
