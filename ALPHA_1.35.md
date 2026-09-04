# Alpha 1.35 — External AI Contract Sync

## Escopo

Sincroniza o contrato exportado em `external_ai_materials_instructions.json` com o mesmo shape aceito pelo validador de retorno de Materials.

## Correção

O contrato anterior descrevia algumas atividades apenas de forma pedagógica, enquanto o importador exigia nomes de campos específicos. Isso permitia uma IA externa produzir JSON coerente com as instruções e ainda assim ser recusado pelo Generator.

Agora o Generator gera um `return_contract` autoritativo com os campos, tipos, limites e condições exatas da resposta esperada. Os blocos legados `*_contract` também são derivados desse mesmo contrato, evitando duas fontes de verdade.

Entre os campos agora explicitados estão:

- `activities.listening_reconstruction[]`: `cue_orders`, `prompt`, `answer`;
- `activities.vocabulary_recall[]`: `cue_orders`, `prompt`, `answer`;
- `activities.structure_transfer[]`: `structure_title`, `prompt`, `model_answers` (2–3);
- `activities.final_listening`: `prompt`, `comprehension_record`;
- Music: `connected_speech_hunt` exatamente `[]` e `shadowing_challenge` exatamente `{}`;
- demais limites de Diagnostic, Structures, Connected Speech, Finalization e Anki.

`snapshot_id`, `generated_at_utc`, cues, timings, Shadowing, Dual Scene e o restante do fluxo não são alterados.
