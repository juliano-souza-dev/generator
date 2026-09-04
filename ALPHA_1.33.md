# Alpha 1.33 — Exact Seven-Block PDF Contract

## PDF único

A capa não conta como bloco. Depois dela, o renderer produz **exatamente**:

1. How To Study
2. Day 5 — Diagnostic
3. Connected Speech
4. Connected Speech — Practice
5. Structures From This Scene
6. Activities
7. Finalization

Não há páginas extras de Anki ou transcrição dentro do PDF. Anki continua separado; Linha a Linha permanece no HUB.

## How To Study

É o bloco 1 e pertence ao Generator. A IA externa não pode reescrevê-lo.

Para Dialogue, o fluxo fixo é:

- **Dia 1:** legenda EN → legenda dupla → Shadowing → Texto + Áudio;
- **Dia 2:** Anki/Cards → Shadowing → Linha a Linha → salvar vocabulário no HUB;
- **Dia 3:** assistir sem legendas → Shadowing → revisar Anki/Cards e repetir somente o que ainda estiver difícil.

## Activities

O contrato exige: Listening Reconstruction, Connected Speech Hunt, Structure Transfer (2–3 frases originais), Vocabulary Recall a partir do Linha a Linha, Shadowing Challenge (ritmo, pausas, entonação) e Final Listening sem legendas com registro de compreensão.

## Connected Speech

Continua vindo apenas da etapa baseada em áudio real + revisão humana, e permanece PDF-only no `hub_final.json`. Explicação e prática ficam em blocos separados.

## Contrato externo

`schema_version = 1.3`. Retornos 1.1/1.2 são recusados.

## Escopo preservado

Não altera timing, player, WbW, Shadowing, Dual Scene nem o `study` canônico do HUB.
