# Alpha 1.34 — Rebuild Materials From Current Contract

## Escopo

Adiciona à etapa `/materials-external` a ação **Refazer materiais** para projetos que já possuem materiais produzidos por uma versão anterior do Generator.

## Comportamento

Ao confirmar **Refazer materiais**, o Generator:

- preserva o JSON canônico final e `snapshot_id`;
- preserva cues, traduções, Word by Word e todos os timings;
- preserva Shadowing;
- preserva a revisão/aprovação de Connected Speech;
- remove somente o retorno/draft de materiais, revisão de materiais, TTS e artefatos finais de materiais;
- recria `external_ai_materials.zip` usando o contrato atual `1.3`;
- inicia automaticamente o download do novo pacote para a IA externa;
- volta a exigir um novo `materials_external_ai_return.json` antes de liberar `/materials-review`.

Nenhum dado canônico é recalculado ou alterado pela ação.
