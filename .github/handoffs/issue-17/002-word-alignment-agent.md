# Issue #17 — 002 Word Alignment Agent

## Gate recebido do Orchestrator
A camada ASR agora possui domínio, áudio técnico 16 kHz mono, adaptador whisper.cpp, persistência atômica e reuso determinístico. O próximo owner não altera Source, corte ou texto por conveniência.

## Objetivo
Refinar timings por palavra a partir do áudio técnico + transcrição EN aceita.

## Entrada
PreparedMaterial válido, áudio técnico local, segmentos/transcrição do ASR e duração do MediaCut.

## Implementação autorizada
- porta WordAligner desacoplada;
- resultado alinhado com start_ms/end_ms por palavra;
- adaptador local de alinhamento em Java;
- versionamento do alinhador incorporado à identidade/snapshot;
- fallback seguro: resultado ASR permanece disponível, mas não pode ser rotulado como alinhamento refinado quando o alinhador falhar.

## Invariantes
0 <= start_ms < end_ms <= duração, ordem monotônica, nenhuma alteração de IN/OUT, nenhum novo download e nenhuma tradução.

## Gate de saída
Testes de invariantes + persistência + falha segura. Depois devolver ao Orchestrator para Experience Validator.
