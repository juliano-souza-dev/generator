# Alpha 1.17 — Connected Speech external package

- Shadowing: `Aprovar e continuar` finaliza o plano aprovado e abre `/connected-speech`.
- Nova etapa 11: Connected Speech para IA externa.
- Pacote contém exatamente:
  1. `ESCOPO_CONNECTED_SPEECH.txt`
  2. `canonical_scene_current.json`
  3. `scene_audio_16k_mono.wav`
  4. `INSTRUCOES_CONNECTED_SPEECH.txt`
- O ZIP é `connected_speech_external_ai.zip`.
- O canônico é copiado byte a byte do resultado atual de WbW Timing; a etapa não altera sua estrutura.
- O mesmo WAV da cena é reutilizado sem novo recorte/reprocessamento.
- O escopo registra IN/OUT local e posição correspondente no vídeo-fonte, além dos blocos de Shadowing aprovados.
- A instrução exige retorno separado `connected_speech_return.json` com `linking`, `elision`, `assimilation`, `reduction` e `contraction` confirmados pelo áudio.
