# Alpha 1.31 — Canonical Identity Lock

- `snapshot_id` e `generated_at_utc` passam a ser metadados de identidade controlados exclusivamente pelo Generator durante o retorno da IA externa.
- No import da etapa de Cues/WbW, qualquer valor devolvido pela IA nesses dois campos é descartado e substituído pelo valor exato do `canonical_scene.json` originalmente exportado.
- A restauração ocorre antes da validação e antes de salvar `external_ai_return.json`.
- Os demais campos protegidos continuam com validação rígida e geram erro se forem alterados.
- As instruções da IA externa reforçam que `snapshot_id` não pode ser regenerado e `generated_at_utc` não pode ser recalculado.
- Nenhuma lógica de timing, revisão, Music, Dialogue, Shadowing, Connected Speech ou materiais foi alterada.
