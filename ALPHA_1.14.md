# Alpha 1.14 — Shadowing HUB preview + global playback shortcuts

## Escopo

Refino da etapa 10 (Shadowing single scene), mantendo o plano de blocos da Alpha 1.13.

## Implementado

- `Space`: reproduz o bloco atual do início até sua PAUSA/END/fim.
- `Shift+Space`: reproduz toda a área de Shadowing.
- Legendas EN/PT visíveis no player de edição.
- Popup de preview antes da finalização, reproduzindo o plano real.
- Fase de vídeo com legenda seguida de `WAITING_REPEAT` com overlay preto 90%.
- Overlay de pausa mostra as cues do bloco em tamanho grande e respeita `pause_duration_ms`.
- Só após concluir o preview o botão **Aprovar e finalizar** é habilitado.
- `RangeEditor` global ganhou `Shift+Space` para reprodução da mídia inteira; `Space` continua sendo a seleção atual.

## Contratos preservados

- PAUSA/END e cálculo de `studentPause` não foram alterados.
- `shadowing.json` permanece com o mesmo schema da Alpha 1.13.
- Nenhuma etapa anterior foi refatorada fora do contrato de atalhos globais.
