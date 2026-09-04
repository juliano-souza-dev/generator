# Alpha 1.21 — Groq readiness source of truth

- Corrige o falso estado **Groq não está pronta** depois de salvar a configuração.
- O backend agora persiste o resultado da validação da Groq junto da configuração, sem expor a API key ao frontend.
- `save_ai_settings()` exige uma API key efetiva, grava, relê o arquivo e confirma que key/modelo/TTS/voz foram persistidos.
- A validação é vinculada à combinação atual de API key + modelo + TTS + voz; alterar qualquer um invalida o estado anterior.
- `/api/materials-groq/state` usa `ai.ready` como fonte de verdade e faz uma validação automática única quando uma configuração antiga ainda não possui o novo estado persistido.
- A etapa 14 mostra `readiness_reason` real em vez de inferir prontidão apenas por `has_api_key`.
- `/api/materials-groq/start` revalida a Groq antes de iniciar a geração.
- Mantido o retorno opcional `return_to=/materials-groq` da Alpha 1.20 apenas como UX.
- Nenhuma mudança em prompts, chunking, Connected Speech, canônico ou geração TTS.
