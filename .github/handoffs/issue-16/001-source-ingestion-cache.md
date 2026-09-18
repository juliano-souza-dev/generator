# Handoff 001 — Issue #16 → Source Ingestion & Cache Agent

## Status
DELIVERED

## Issue
#16 — [P0][M1][Bug] Source do YouTube falha de forma intermitente e exige múltiplas tentativas

## Branch autorizada
`fix/issue-16-source-youtube-reliability`

## Base
`main@a77ca74aed22acd76bfb6569e176af16044a3670`

## Owner
Source Ingestion & Cache Agent

## Evidência do QA humano
A mesma URL de YouTube falhou em várias tentativas de “Preparar fonte” e depois funcionou sem mudança na URL.

Diagnóstico correto:
- Source não está totalmente quebrado;
- problema é intermitência/confiabilidade;
- usuário não deve precisar clicar repetidamente.

## Contratos imutáveis
- consultar Source Cache antes de qualquer inspeção/download;
- cache hit válido não baixa novamente;
- SourceMedia permanece o contrato de saída;
- original persistido no cache não é sobrescrito por consumidores;
- UI continua em linguagem de produto, sem expor yt-dlp/FFmpeg;
- Wave e Timing estão fora do escopo.

## Trabalho autorizado

### 1. Retry interno
- uma ação “Preparar fonte” deve absorver falhas transitórias;
- retry limitado;
- backoff pequeno e determinístico/testável;
- erro claramente definitivo não deve ser repetido inutilmente.

### 2. Download resiliente
- fornecer FFmpeg explicitamente ao yt-dlp;
- estratégia primária deve preferir vídeo+áudio e saída MP4;
- implementar fallback de formato quando a primeira estratégia não produzir mídia;
- não deixar arquivos incompletos virarem cache válido.

### 3. Diagnóstico
- registrar detalhes técnicos por tentativa em arquivo sob `AppDirectories.logsDir()`;
- UI recebe apenas erro simples de produto;
- distinguir inspect, download/formato e falha definitiva no log.

### 4. Testes
Sem rede:
- falha transitória seguida de sucesso;
- falha definitiva sem retries extras;
- fallback de formato;
- cache hit continua sem inspect/download;
- falha total não cria falso cache.

## Arquivos principais
- `desktop/src/main/java/.../source/YtDlpSourceDownloader.java`
- `desktop/src/main/java/.../source/SourceAcquisitionService.java`
- `desktop/src/main/java/.../source/SourceModule.java`
- testes de Source em `desktop/src/test/java/.../source/`

## Fora de escopo
- Wave/Timing;
- nova janela;
- mudança no SourceMedia;
- download remoto via backend/API;
- alterações da Milestone 2.

## Gate
Orquestrador aceita apenas com testes verdes e evidência de que uma única ação do usuário cobre a intermitência transitória.

## Delivery evidence

- JDK 21 local compilation: PASS;
- deterministic transient retry harness: PASS;
- definitive failure without retry harness: PASS;
- format fallback + explicit FFmpeg harness: PASS;
- Source Cache hit avoids inspect/download harness: PASS;
- JUnit regression tests added to the branch;
- PR #19 opened for CI/QA;
- GitHub Actions did not start a check for the PR head at the time of delivery, so no CI PASS is claimed.
