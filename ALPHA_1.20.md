# Alpha 1.20 — Groq config return flow

- `Abrir configuração` na etapa 14 abre `/config?return_to=/materials-groq`.
- Depois de salvar e validar a Groq, a configuração retorna automaticamente à etapa 14.
- `return_to` é limitado a uma allowlist local para evitar redirecionamento arbitrário.
- A etapa Groq relê o estado ao voltar pelo BFCache do navegador.
- Nenhuma mudança em materiais, prompts, chunking, TTS, Connected Speech ou canônico.
