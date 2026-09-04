# Alpha 1.7 — Word by Word review

Nova etapa após a revisão humana de 100% das cues.

## Fluxo

`IA Externa → Revisar cues → Word by Word`

A página `/word-review` só é liberada quando todas as cues da etapa anterior estiverem marcadas como aceitas.

## Revisão por word

- uma word aparece por vez;
- a caixa EN edita `cues[].words[].text`;
- a caixa PT edita a tradução da word;
- **Salvar** marca a word como humanamente revisada (`review_status=approved`) e avança para a próxima pendente;
- `human_verified_audio` continua `false`, pois esta etapa valida texto/tradução e não representa validação auditiva de timing;
- o progresso persiste no workspace.

## Cues e navegação

A lateral lista todas as cues. Cada cue mostra quantas words já foram aceitas e fica `ACEITA` somente quando 100% das suas words tiverem sido validadas.

Na área principal existe também uma faixa com todas as words da cue atual, permitindo voltar a qualquer word já aceita ou pendente sem mostrar duas words simultaneamente no editor.

## pt_group

A estrutura canônica existente é preservada.

Quando uma word pertence a uma unidade semântica `pt_group`, todas as words do grupo continuam sendo validadas individualmente, mas a caixa PT edita a tradução única armazenada na word `lead`. As words `member` continuam com `pt=null`.

Nenhum campo, wrapper ou schema novo é criado para grupos de tradução.

## Conclusão

Quando 100% das words de 100% das cues estiverem aceitas, a página rola suavemente até o bloco **Próxima etapa**. Essa próxima etapa ainda não foi implementada nesta Alpha.

A cópia canônica desta etapa é salva no workspace como `word_review/canonical_scene_word_reviewed.json`.
