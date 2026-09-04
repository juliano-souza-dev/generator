# Alpha 1.11 — WbW Time

- Nova etapa 09 `WbW Time` após Cue Timing.
- Reutiliza o WaveEditor global, zoom 1×–8×, velocidade, IN/OUT e atalhos.
- Words com o mesmo `pt_group` são tratadas como uma única unidade semântica, sem alterar o schema canônico.
- Preview no vídeo exibe EN em cima e PT embaixo. A unidade ativa recebe o mesmo destaque nos dois idiomas.
- O timing em edição aparece imediatamente no preview antes de salvar.
- `G` salva e avança para a próxima unidade; `Space` reproduz a unidade; `Shift+Space` reproduz a cena.
- Em grupos, o novo intervalo é aplicado proporcionalmente aos `start_ms/end_ms` das words membros, preservando seus `original_*`.
