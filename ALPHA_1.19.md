# Alpha 1.19 — Groq Materials Content + TTS

## Escopo

Nova etapa **14 · Groq · Materiais**, imediatamente após a conferência manual de Connected Speech.

## Contrato

- O JSON canônico final é **somente leitura** nesta etapa.
- `connected_speech_review.json` entra somente com itens `decision=approved` no Connected Speech Lab.
- A Groq gera conteúdo pedagógico estruturado para:
  - Study Workbook;
  - Guide / Answer Key;
  - Anki;
  - Connected Speech Lab.
- Transcript Original e Transcript Bilingual são derivados localmente do canônico, sem pedir à IA para reescrever falas/traduções.
- O resultado pedagógico fica em `materials_groq_draft.json`, separado do canônico.

## Groq — mesma arquitetura antiga

- SDK oficial `groq`.
- API key salva localmente ou via `GROQ_API_KEY`; nunca devolvida ao navegador.
- modelo padrão `openai/gpt-oss-120b`.
- TTS padrão `canopylabs/orpheus-v1-english`.
- vozes: autumn, diana, hannah, austin, daniel e troy.
- preflight via `models.list()`.
- JSON mode com fallback para modelos incompatíveis.
- adaptação automática a limites de `max_completion_tokens` retornados pela API.
- retry de 429 respeitando `Retry-After`.
- orçamento seguro padrão de 6500 tokens estimados por janela de 60s para contas com limite de 8k TPM.
- chunking por cues e persistência dos fragmentos em disco; uma retomada não repete blocos já concluídos.

## Anki

- 0–2 cards por cue.
- no máximo 10 cards fortes no draft.
- não força quantidade quando o material é fraco.
- `cue_order` obrigatório e existente.
- `marked` deve existir em `highlight_en` e `markedPT` em `highlight_pt`.
- deduplicação local de `focus`.

## Áudio

Depois do conteúdo Groq ser validado localmente:

- o Generator identifica as cues realmente usadas pelos cards;
- gera **um WAV Groq TTS por cue de origem**;
- cards da mesma cue reutilizam o mesmo áudio;
- TTS é retomável: WAV já válido não é gerado novamente;
- gera `tts_manifest.json` e `materials_tts_audio.zip`.

O áudio original da cena não é usado como áudio dos cards.
