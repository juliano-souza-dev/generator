# Alpha 1.30 — Music Micro-Immersion Flow

- Libera `content_type=music` sem alterar o ramo `kit/dialogue`.
- Music usa o mesmo Wave Editor global e exige recorte manual de 30–60 segundos.
- External AI etapa 05 recebe instruções específicas de lyrics + tradução + WbW somente do microtrecho.
- Cue/Word Review e Cue/WbW Timing reutilizam os mesmos editores; speaker é ocultado em Music.
- Após WbW Timing, Music pula Shadowing e Connected Speech e segue direto para materiais externos.
- Pacote de materiais usa CS vazio determinístico e bloqueia criação de Connected Speech para Music.
- Connected Speech Lab fica automaticamente rejeitado na revisão de materiais Music.
- `hub_final.json` Music preserva `scene_start_ms`, `scene_end_ms`, `scene_duration_ms`, exige 30–60s e sempre inclui `music.sections`.
- Groq continua somente TTS.
- Navegação Music é compactada visualmente para 12 etapas; 10–13 do ramo dialogue ficam ocultas e materiais viram etapas 10/11/12.
- O canônico Music inclui `youtube_video_id` e `youtube_title` como metadados protegidos para identificação da versão correta pela IA externa.
- O JSON Music final não publica `shadowingConfig`/`shadowingPractice`; `connectedSpeech` fica vazio e o HUB usa somente a microjanela oficial.
- `hub_final.json` Music foi validado no preflight do Admin HUB v109 sem erros bloqueantes.
