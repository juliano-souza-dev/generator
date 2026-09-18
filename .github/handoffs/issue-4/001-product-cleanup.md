# Handoff 001 — Issue #4 → Product Cleanup Agent

## Status
DELIVERED

## Issue
#4 — [M2] Remover completamente o Dual Scene do produto e do código

## Branch autorizada
`cleanup/issue-4-remove-dual-scene`

## Base
`main@d3bb11bff5288fbbe1934382909376a4b1a6c7cf`

## Decisão de produto
Dual Scene foi descontinuado integralmente.

**Não manter compatibilidade legada.**
**Não criar adaptador.**
**Não preservar leitura especial de projetos antigos.**
**Não migrar projetos antigos.**

## Inventário inicial confirmado

### app.py
Dual Scene aparece em:
- diretórios/arquivos `DUAL_SCENE_*`;
- `configuration.dual_scene`;
- estado raiz `dual_scene`;
- estratégia de download/processamento;
- JSON técnico inicial;
- resume/navigation após mídia;
- payload do Word Timing;
- helpers `_dual_scene_*`;
- gates de Shadowing/Connected Speech/materials;
- final materials;
- `ConfigureRequest.dual_scene`;
- `DualSceneBlocksRequest`;
- página `/dual-scene`;
- APIs `/api/dual-scene/*`;
- mensagens de materiais.

### Frontend
- `static/dual_scene.html`;
- `static/dual_scene.js`;
- `static/app.js` ainda envia `dual_scene:false` e possui fallback para `/dual-scene`;
- `static/word_timing.js` oferece navegação para Dual Scene;
- `static/materials_final.js` lista artefato Dual Scene.

### Renderização/importação
- `materials_final.py` contém renderer, validação e artefato `videoDualScene.mp4`;
- `final_project_import.py` importa/restaura `dualScene`.

### Testes
- `tests/test_dual_scene_download_quality.py`;
- `tests/test_dual_scene_subtitle_layout.py`;
- `tests/test_final_project_import.py` possui caso com `dual_scene=True`.

### Observação crítica
`materials_final._write_dual_scene_ass` também é usado por Music. Não apagar automaticamente. Se permanecer necessário para Music, renomear para um nome neutro e atualizar consumidores/testes.

## Fluxo esperado após remoção
- configuração não possui Dual Scene;
- pós-WbW segue apenas para Shadowing ou materiais quando o fluxo atual permitir;
- materiais dependem de Shadowing, não de Dual Scene;
- JSON final não publica `dualScene`;
- renderização final não gera `videoDualScene.mp4`;
- importador não reconhece/restaura Dual Scene;
- nenhuma rota/página/API Dual Scene existe.

## Fora de escopo
- refatorar Source/Wave;
- implementar M3;
- remover Connected Speech nesta issue;
- alterar fluxo Music além de retirar dependência nominal de Dual Scene;
- limpeza Python genérica da #6.

## Gate
Orquestrador só aceita após:
- implementação removida;
- testes relevantes ajustados;
- busca final por identificadores Dual Scene sem implementação ativa;
- Experience Validator confirmar navegação coerente;
- QA revisar regressões.

## Delivery evidence

### Removed
- root/config state and PT source exclusive to the retired feature;
- workspace/media constants and preparation branches;
- backend helpers, page and API routes;
- frontend page/script/navigation and dedicated CSS;
- final renderer/artifact/HUB transport behavior;
- final-project import behavior;
- dedicated tests.

### Preserved
- shared WbW subtitle rendering, renamed to `_write_wbw_ass`;
- Music renderer;
- regular YouTube 720p regression coverage;
- Shadowing flow.

### Regression coverage added/updated
- retired configuration field is rejected;
- retired page/API routes are absent;
- current same-kit reconfiguration preserves reviewed state;
- WbW vertical subtitle layout/highlight remains covered;
- regular YouTube quality policy remains covered.

### Search evidence
Functional backend/frontend/render/import/template files were scanned after the change with no implementation references remaining.

Governance documents under `.github/handoffs/issue-4/` intentionally retain the removed feature name as audit evidence.

### Test execution limitation
No Python CI workflow is configured to run for PR #20, and no automated run was started. Test execution PASS is therefore **not claimed**.
