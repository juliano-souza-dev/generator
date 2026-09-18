# Handoff 003 — Issue #12 → Timing Editor Agent

## Status
READY — liberado pelo Orquestrador após aceite do Source Ingestion & Cache Agent.

## Dependência aceita
Source Agent aprovado no GitHub Actions run `35366298728`.

O Timing Editor recebe um `SourceMedia` já local, válido e persistido. Nenhum download de Source é permitido nesta etapa.

## Issue
#12

## Branch prevista
`feat/java-desktop-restart`

## Entrada congelada
`SourceMedia`, definido em `.github/contracts/source-media.md`.

O Wave recebe mídia local. Ele não baixa novamente a URL e não modifica o arquivo original no Source Cache.

## Referência funcional coletada pelo Orquestrador
O Wave legado possuía:
- vídeo + waveform;
- seletores IN/OUT arrastáveis;
- playhead;
- edição textual de IN/OUT;
- reprodução da seleção;
- duração;
- atalhos Space, A, S e nudges;
- zoom/pan no componente global.

O README atual registra:
- IN/OUT sempre visíveis;
- `Space` play/pause;
- `A` IN;
- `S` OUT;
- setas ±10ms;
- Shift ±100ms;
- Alt ±1ms.

## Regras para a versão Java
- não portar HTML/JS;
- reimplementar comportamento nativamente;
- uma única Stage/Scene;
- playback usa `SourceMedia.localPath`;
- o original é somente leitura;
- toda mídia derivada/cortada vai para diretório de projeto/workspace, nunca para Source Cache;
- nenhum timing pode sair da duração real da mídia.

## Contrato de saída proposto
`MediaCut`
- sourceId;
- sourcePath;
- outputPath;
- startMs;
- endMs;
- durationMs.

## Critérios de aceite
- [ ] playback local funcional;
- [ ] waveform real;
- [ ] IN/OUT por drag;
- [ ] edição precisa em ms;
- [ ] atalhos validados;
- [ ] zoom/pan;
- [ ] preview da seleção;
- [ ] persistência do recorte;
- [ ] arquivo derivado não altera Source Cache;
- [ ] testes de invariantes;
- [ ] mesma janela durante todo o fluxo;
- [ ] entrega ao Orquestrador.

## Fora de escopo
ASR, Cue Timing, WbW Timing, Shadowing e qualquer issue da Milestone 2.
