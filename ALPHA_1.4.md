# Alpha 1.4 — pacote para IA externa

Nova etapa após o processamento técnico da mídia.

## Fluxo

`/process` pronto → **Avançar para IA Externa** → `/external-ai`

A página `/external-ai` prepara quatro downloads visíveis:

1. `canonical_scene.json`
2. `scene_audio_16k_mono.wav`
3. `INSTRUCOES_EXTERNAL_AI.txt`
4. `external_ai_scene.zip`

O ZIP contém exatamente os três primeiros arquivos.

## Contrato canônico

- schema: `immersionhub-canonical-ai-input`
- schema_version: `1.7`
- o Generator gera `cues: []` nesta etapa;
- a IA externa ouve o áudio e preenche somente `cues[]` com transcrição EN, tradução PT, speaker, timestamps e Word by Word;
- grupos semânticos PT usam somente `pt_group`, `pt_group_role`, `pt_group_original_pt`.

Campos raiz protegidos contra alteração externa:

`schema`, `schema_version`, `snapshot_id`, `generated_at_utc`, `workflow_status`, `generator`, `project`, `review`, `shadowingConfig`, `ai_training`.

## Validação do retorno

O drag-and-drop do JSON valida:

- raiz e campos protegidos;
- snapshot e schema;
- cues sequenciais;
- timings de cue dentro da timeline local da cena;
- `original_en`, `approved_en`, `pt`;
- Word by Word completo e dentro do cue;
- campos atuais do WbW;
- `review_status=pending` e `human_verified_audio=false`;
- tradução individual ou grupos PT válidos.

A próxima etapa ainda não é implementada nesta Alpha. O estado fica marcado como validado e pronto para conexão com o próximo fluxo.
