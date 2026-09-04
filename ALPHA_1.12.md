# Alpha 1.12 — WbW group edit + free timing

- Agrupar/desagrupar unidades WbW manualmente na etapa 07, mantendo a mesma cue.
- Sem schema novo: continua usando `pt_group`, `pt_group_role` e `pt_group_original_pt`.
- Desagrupar restaura traduções individuais conhecidas e deixa pendente o que a IA externa não forneceu.
- Agrupamentos alterados voltam a pendente e invalidam Cue Timing/WbW Time.
- WbW Time usa o intervalo da cue apenas como seed inicial; depois permite ultrapassar IN/OUT da cue até os limites da mídia.
- `original_start_ms` e `original_end_ms` permanecem intocados.
- Botão “Avançar para Word by Word” normalizado para o padrão visual do fluxo.
