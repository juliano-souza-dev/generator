# Alpha 1.32 — Single PDF Contract

## Escopo

Esta versão altera somente o fluxo de materiais/PDF da Alpha 1.31. Timeline, cues, Word by Word, Shadowing, Dual Scene, Music timing e player não foram refatorados.

## PDF único

A geração final passa a produzir um único documento:

`01_immersionhub_workbook.pdf`

Ordem fixa:

1. Workbook
2. How To Study
3. Guide / Answer Key
4. Original English Transcript
5. English + Portuguese Line by Line
6. Connected Speech Lab, somente quando existirem itens aprovados

A capa é controlada pelo Generator e contém a marca ImmersionHub, o título do kit e o lema:

`Less time preparing. More time actually practicing.`

## How To Study

O plano deixa de ser gerado pela IA externa. O Generator injeta um fluxo fixo de 3 dias e o apresenta como bloco protegido durante a revisão, evitando drift pedagógico entre kits.

## Connected Speech

Connected Speech permanece fora do HUB (`connectedSpeech: []`) e passa a existir somente dentro do workbook PDF. Em Music, a seção é omitida.

## Contrato da IA externa

O retorno esperado muda para `schema_version: 1.1` e aceita somente:

`pdf_content.immersion_workbook`

Esse objeto contém:

- `study_workbook`
- `guide_answer_key`
- `connected_speech_lab`

Capa, How To Study e transcrições são derivados pelo Generator e não devem ser devolvidos pela IA.

## Identidade protegida

As garantias da Alpha 1.31 permanecem válidas. `snapshot_id` e `generated_at_utc` do canônico não podem ser regenerados ou alterados pela IA externa; `source_snapshot_id` deve copiar exatamente o `snapshot_id` canônico.

## Arquivos finais

- `01_immersionhub_workbook.pdf`
- `02_anki_sem_audio.apkg`
- `03_anki_com_tts.apkg`
- `hub_final.json`
- `materials_final.zip`
