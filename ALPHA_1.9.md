# Alpha 1.9 — Cue Timing shortcuts + subtitle preview

- `Shift + Space` reproduz/pausa a cena processada inteira (0 → fim) na etapa Cue Timing.
- `G` salva a cue atual e reutiliza o fluxo existente para carregar automaticamente a próxima cue.
- O vídeo da etapa Cue Timing agora mostra legendas EN + PT sincronizadas.
- Para a cue atual, o preview usa o IN/OUT em edição imediatamente, antes do save.
- O `Space` continua reproduzindo somente a seleção IN/OUT da cue atual.
- O WaveEditor ganhou apenas um método público de cancelamento de playback para evitar conflito entre reprodução da seleção e reprodução integral.
