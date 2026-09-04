# Alpha 1.13 — Shadowing single-scene pause blocks

- Bifurcação após WbW Time por `dualScene`.
- `dualScene=false` abre `/shadowing`; `dualScene=true` permanece reservado para a próxima implementação.
- Editor global por marcadores: PAUSA fecha o bloco e o próximo começa exatamente no mesmo ponto.
- END encerra o último bloco antecipadamente; sem END, o plano usa o vídeo completo.
- Cada bloco calcula a pausa de prática com `shadowingConfig.studentPause`.
- Persistência em `workspace/shadowing/shadowing.json`, incluindo timings relativos/absolutos e `cue_orders`.
- Atalhos: `Space` play/pause, `A` PAUSA, `E` END, `Del` remove selecionado, setas ±10 ms, `Shift` ±100 ms, `Alt` ±1 ms.
- Zoom 1×–8×, pan e controle de velocidade mantidos.
