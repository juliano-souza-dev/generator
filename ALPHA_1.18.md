# Alpha 1.18 — Connected Speech return + manual review

## Escopo

Implementa as duas etapas posteriores ao pacote de Connected Speech, sem alterar o JSON canônico revisado.

### 12 · Validar retorno
- Recebe `connected_speech_return.json` por clique ou drag-and-drop.
- Valida schema/version, `source_snapshot_id`, SHA-256 do canônico, `audio_scope`, presença única de todos os cues do escopo, tipos permitidos, `sequenceOrder`, campos obrigatórios, confiança e limites de timing por cue.
- Em caso de erro, o retorno é recusado com motivo explícito.
- Em caso de sucesso, persiste o retorno validado e segue automaticamente para a conferência manual.

### 13 · Conferência CS
- Achata os fenômenos validados em uma fila global por `sequenceOrder`.
- Exibe um item por vez com cue, tipo, intervalo, confiança, trecho ouvido e explicação.
- `Aprovar` e `Recusar` persistem a decisão imediatamente e avançam para o próximo item pendente.
- Itens anteriores podem ser reabertos e ter a decisão alterada.
- O canônico e o JSON devolvido pela IA não são sobrescritos.
- As decisões são gravadas separadamente em `connected_speech_review.json`.
