# Alpha 1.2 — Wave Editor global + layout desktop em duas colunas

Escopo desta release:

- cada fluxo continua em uma página própria;
- em `/wave`, desktop usa duas colunas: vídeo à esquerda e WaveCut/Wave Editor à direita;
- player desktop preserva exatamente 812 × 397 px;
- abaixo de 1180 px o conjunto empilha; no mobile o vídeo ocupa 100% da largura mantendo a proporção 812:397;
- a barra horizontal de etapas fica sticky imediatamente abaixo da topbar durante a rolagem;
- todo `RangeEditor` segue o contrato global de Wave Editor:
  - seletor IN visível e arrastável;
  - seletor OUT visível e arrastável;
  - timecodes IN/OUT editáveis;
  - último Wave Editor usado vira o editor global ativo;
  - atalhos funcionam globalmente sem exigir foco no canvas;
- atalhos globais:
  - `Space`: play/pause da seleção;
  - `A`: IN no playhead;
  - `S`: OUT no playhead;
  - `←/→`: handle ativo ±10 ms;
  - `Shift+←/→`: ±100 ms;
  - `Alt+←/→`: ±1 ms.

Não foram adicionadas novas etapas de processamento nesta release.
