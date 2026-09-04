# Alpha 1.15 — Shadowing shortcuts contract hardening

## Escopo

Fechamento do contrato de atalhos da etapa Shadowing sem alterar schema, cálculo de pausas ou demais etapas.

## Implementado

- `Space` continua reproduzindo o bloco atual no Shadowing.
- `Shift+Space` continua reproduzindo a cena inteira no Shadowing.
- O `MarkerEditor` compartilhado agora também reconhece `Shift+Space` como reprodução da mídia inteira caso o handler específico da página não intercepte o evento.
- Preview obrigatório antes de salvar permanece: vídeo + legendas → `WAITING_REPEAT` com overlay preto 90% e cues grandes → próximo bloco.
- Nenhum contrato persistido foi alterado.

## Preservado

- `shadowingConfig.studentPause` e `pause_duration_ms`.
- `shadowing.json` e JSON canônico.
- Fluxos anteriores do Generator.
