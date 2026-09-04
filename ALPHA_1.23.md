# Alpha 1.23 — IA externa para PDF/Anki + Groq somente TTS

## Mudança de responsabilidade

A geração pedagógica saiu completamente da Groq.

Fluxo:

`13 Conferência CS → 14 IA Externa · Materiais → 15 Revisão PDFs + Anki → 16 Groq TTS + renderização final`

### 14 · IA Externa · Materiais

O Generator cria `materials_external_ai_package.zip` com exatamente:

- `canonical_scene_current.json` — canônico humano, somente leitura;
- `connected_speech_review.json` — resultado final da conferência de Connected Speech;
- `external_ai_materials_instructions.json` — contrato pedagógico baseado no Generator antigo.

A IA externa devolve `materials_external_ai_return.json` com o conteúdo de Study/PDF e Anki.

O contrato recupera as regras antigas de materiais: conteúdo completo e específico, sem placeholders; máximo de 2 cards por cue; `marked`/`markedPT` literais; `example_en` novo; focus sem duplicação; módulos cobrindo os cues; active recall, listening, produção, retell e plano de repetição.

O contrato antigo de Study (`repeatDays`, `methodCycle`, `centralRule`, `modules`, `listeningChallenges`, `finalRetell`, `sevenDayPlan`, `transcriptNote`, `translationNote`) fica explicitamente documentado nas instruções e mapeado para os PDFs atuais.

Connected Speech já foi aprovado pelo humano. A IA externa não pode criar, excluir ou reclassificar fenômenos; apenas acrescenta conteúdo pedagógico de prática para os itens `approved`.

As transcrições Original e Bilingual são derivadas localmente do canônico, como no contrato antigo.

### 15 · Revisão PDFs + Anki

O retorno externo validado alimenta diretamente a revisão humana. PDF e cards continuam podendo ser aprovados, recusados ou editados antes da produção.

Nenhum TTS é criado antes dessa revisão.

### 16 · Groq TTS + geração final

A Groq é usada exclusivamente para TTS. A configuração também foi simplificada: o estado `ready` não depende mais de modelo de análise, apenas de API key + modelo TTS + voz + conexão válida.

- somente cards Anki aprovados entram na fila;
- cada `cue_order` gera no máximo um WAV reutilizável;
- cache é invalidado quando revisão, modelo TTS ou voz mudam;
- o conteúdo pedagógico nunca é enviado ao modelo de análise da Groq.

Depois dos WAVs, o Generator renderiza localmente PDFs, APKG sem áudio, APKG com TTS e ZIP final.

## Compatibilidade

`/materials-groq` permanece somente como redirect legado para `/materials-external`. A engine antiga de geração pedagógica via Groq foi removida do pacote.
