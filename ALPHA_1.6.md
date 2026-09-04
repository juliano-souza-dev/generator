# Alpha 1.6 — human cue text review

Nova etapa após a validação do JSON da IA externa.

## Fluxo

`IA externa validada → Revisar cues`

A página `/cue-review` apresenta uma cue por vez e mantém todas as cues na lateral.

## Revisão

- caixa EN editável salva em `cues[].approved_en`;
- caixa PT editável salva em `cues[].pt`;
- `cues[].original_en` permanece intacto;
- cada cue começa como `PENDENTE`;
- clicar em **Salvar** marca a cue como `ACEITA` e abre a próxima pendente;
- cues aceitas continuam acessíveis pela lista lateral e podem ser salvas novamente;
- o progresso persiste no workspace.

## Restaurar da IA

O retorno original da IA externa permanece preservado em `external_ai_return.json`.

O botão **Restaurar da IA** aparece somente quando EN ou PT da cue aberta divergem dos valores originalmente retornados pela IA. O botão repõe os dois valores nas caixas; o usuário ainda precisa clicar em **Salvar** para validá-los.

## Conclusão

Quando todas as cues estiverem aceitas, a página rola suavemente até o bloco de conclusão. A próxima etapa ainda não foi implementada nesta Alpha.

A cópia canônica humanamente revisada permanece com a mesma estrutura e é salva no workspace como `cue_review/canonical_scene_reviewed.json`.
