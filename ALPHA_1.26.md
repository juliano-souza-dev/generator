# Alpha 1.26 — HUB WbW timing transport contract

## Escopo

Patch isolado na geração e validação de `hub_final.json`. O JSON canônico interno, WbW revisado e HUB não são alterados.

## Correção

- Cues com `words[]` passam a publicar `speech_start_ms` / `speech_end_ms` como única autoridade temporal do cue no JSON do HUB.
- `subtitle_start_ms` / `subtitle_end_ms` são removidos somente desses cues no transporte final, evitando que o importador do HUB priorize um limite de legenda mais estreito que o WbW real.
- Cues sem WbW preservam o comportamento anterior.
- Antes de escrever `hub_final.json`, cada word é validada contra o intervalo `speech_*` do próprio cue.
- O Generator não faz clamp/correção silenciosa. Se houver erro real de WbW, a geração final é bloqueada e mostra cue, posição, palavra e os dois intervalos em ms.

## Validação

- Sintaxe Python validada.
- Teste de regressão confirma que um cue com subtitle mais curto, mas WbW corretamente contido em `speech_*`, é publicado sem `subtitle_*` e passa na validação.
- Teste negativo confirma que word realmente fora de `speech_*` é bloqueada com erro explícito.
