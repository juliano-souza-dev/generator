# Alpha 1.25 — Final groups + HUB JSON

## Escopo

A etapa 16 foi ajustada sem alterar as etapas de revisão, TTS ou geração pedagógica.

## Alterações

- Downloads finais separados por categoria:
  - JSON do HUB;
  - PDFs;
  - Anki;
  - áudios/manifest TTS;
  - pacote completo.
- Geração automática de `hub_final.json`.
- `hub_final.json` também é incluído em `materials_final.zip`.
- O JSON do HUB usa:
  - cues, timings e Word by Word do canônico final;
  - apenas cards Anki aprovados na revisão;
  - `shadowingPractice` aprovado;
  - conteúdo `study` aprovado;
  - janela técnica `scene_start_ms`, `scene_end_ms` e `scene_duration_ms` do projeto.
- Connected Speech permanece PDF-only e é publicado como `connectedSpeech: []` no HUB.
- `pt_group_original_pt` é removido do JSON de publicação por ser metadado interno do editor.
- Download `.json` servido com `application/json`.

## Validação

- Python e JavaScript passaram em verificação de sintaxe.
- `hub_final.json` foi validado localmente e passou no `preflight()` do importador do HUB v103 sem `blockingErrors`.
