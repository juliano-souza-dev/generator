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
- derivados locais de preview;
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
- playback, waveform e seleção devem representar a mesma timeline;
- presença de mídia válida não garante compatibilidade direta com JavaFX MediaView;
- quando necessário, deve produzir preview local compatível e reutilizável sem substituir a fonte;
- preview derivado deve preservar duração/timeline para que playhead, seleção, waveform e vídeo continuem sincronizados.

## Maturidade acumulada
- O caso real de Wave com waveform/duração corretas e tela preta deve ser tratado como falha de decode/render do preview, não como ausência de mídia.
- O caminho seguro é manter a fonte canônica intacta e derivar preview H.264/AAC compatível para reprodução.
- A existência do arquivo ou ausência de exceção no construtor do MediaPlayer não é evidência suficiente de imagem renderizável; validar estado ready/error e smoke com vídeo real.

## Saída
- implementação;
- testes;
- contrato do recorte/preview derivado;
- arquivos alterados;
- evidências;
- riscos;
- handoff ao Orquestrador.

## Encerramento
O agente não libera Experience Validator. Entrega ao Orquestrador para aceite formal.

## Experiência de produto consolidada — 2026-09-19
- Wave só é funcional quando vídeo, áudio, waveform, playhead e seleção representam a mesma timeline e são perceptivelmente utilizáveis.
- Waveform/duração corretas com frame preto continuam sendo falha de produto.
- Incompatibilidade de codec/container deve ser resolvida com preview derivado local e reutilizável, preservando a fonte original.
- Smoke deve exercitar ao menos uma origem que force o caminho de compatibilidade, não apenas um MP4 já amigável.
- Salvar recorte deve preservar exatamente IN/OUT e produzir MediaCut reutilizável por projeto.
- Reabrir projeto pós-corte deve restaurar recorte/drafts sem reexecutar Source nem refazer o corte.
- Nenhum preview, corte ou waveform pode disparar novo download.
- UI não expõe FFmpeg, codec, container ou detalhes internos.
