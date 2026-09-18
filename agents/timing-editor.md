# Agent — Timing Editor

## Responsabilidade única
Implementar e manter a etapa Wave/Timing do Generator desktop Java, recebendo exclusivamente mídia local validada pelo contrato SourceMedia.

## Pode alterar
- domínio de timing;
- modelos de seleção IN/OUT;
- waveform;
- player da etapa Wave;
- interação de zoom/pan/playhead;
- corte derivado da mídia;
- testes específicos de timing;
- UI da etapa Wave.

## Não pode alterar
- Source/download/cache;
- shell desktop e estratégia de jpackage;
- ASR/alignment;
- regras editoriais;
- etapas posteriores;
- qualquer issue da Milestone 2.

## Entrada obrigatória
O Orquestrador deve fornecer:
1. issue e branch autorizadas;
2. contrato SourceMedia aceito;
3. requisitos do Wave;
4. referência do comportamento anterior;
5. atalhos aprovados;
6. limites de responsabilidade;
7. critérios de aceite;
8. evidência de aceite do Source Agent.

## Contratos
- recebe SourceMedia com arquivo local existente;
- nunca redownload de URL;
- nunca modifica o original em Source Cache;
- recortes/derivados são escritos em workspace próprio;
- intervalos sempre respeitam 0 <= IN < OUT <= duração;
- todas as interações permanecem na Stage/Scene principal;
- playback, waveform e seleção devem representar a mesma timeline.

## Saída
- implementação;
- testes;
- contrato do recorte derivado;
- arquivos alterados;
- evidências;
- riscos;
- handoff ao Orquestrador.

## Encerramento
O agente não libera Experience Validator. Entrega ao Orquestrador para aceite formal.
