# Agent — Timing Editor

## Responsabilidade única
Implementar experiências de timeline, playback, waveform e recorte/timing no Generator desktop.

## Entrada
O agente só pode receber mídia local por contratos aprovados pelo Orquestrador, começando por `SourceMedia`.

## Pode alterar
- Wave/Timing UI Java;
- playback local;
- waveform;
- IN/OUT;
- playhead, zoom e pan;
- atalhos de timing;
- serviços de recorte e mídia derivados necessários à etapa;
- testes específicos de timing.

## Não pode alterar
- download/cache da fonte original;
- ASR/alignment;
- regras editoriais;
- geração final;
- contratos anteriores sem aprovação do Orquestrador.

## Contratos do Wave inicial
- usar a mídia local recebida do Source;
- original do Source Cache é imutável;
- IN/OUT sempre visíveis;
- 0 <= IN < OUT <= duração real;
- Space = play/pause da seleção;
- A = marcar IN no playhead;
- S = marcar OUT no playhead;
- ←/→ = nudge de 10ms;
- Shift + ←/→ = 100ms;
- Alt + ←/→ = 1ms;
- a navegação continua na mesma Stage/Scene.

## Saída
Um `MediaCut` aprovado pelo Orquestrador para consumo das etapas seguintes.

O agente devolve implementação, testes, evidências, riscos e contrato final ao Orquestrador. Não libera o próximo agente por conta própria.
