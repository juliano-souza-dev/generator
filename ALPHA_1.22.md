# Alpha 1.22 — revisão de PDFs/Anki + geração final

## Fluxo

A saída da etapa 14 agora é uma fonte de revisão, não um produto final:

`Groq · Materiais → Revisão PDFs + Anki → Geração final`

### 14 · Groq · Materiais

- continua gerando `materials_groq_draft.json` e os WAV TTS;
- ao concluir, informa `next_url=/materials-review` e a UI segue para a etapa 15;
- o aviso visual de Groq não pronta fica oculto quando a geração já está em execução/pronta;
- nenhuma renderização PDF/APKG acontece nesta etapa.

### 15 · Revisão PDFs + Anki

- 5 fontes de PDF são revisadas separadamente:
  - Study Workbook;
  - Guide / Answer Key;
  - Transcript Original;
  - Transcript Bilingual;
  - Connected Speech Lab;
- cada PDF pode ser aprovado, recusado ou editado em JSON antes da decisão;
- cada card Anki pode ser aprovado/recusado individualmente;
- campos pedagógicos do card são editáveis;
- o WAV TTS do cue fica disponível na própria revisão do card;
- editar um item o devolve para `pending`;
- a revisão só finaliza quando não houver pendências;
- saída persistida: `workspace/materials_review/materials_review.json` e `materials_review_approved.json`.

### 16 · Geração final

- abre após a revisão e inicia automaticamente;
- usa somente conteúdo aprovado;
- não faz nova chamada pedagógica à Groq;
- gera localmente os PDFs aprovados;
- quando houver cards aprovados, gera:
  - `06_anki_sem_audio.apkg`;
  - `07_anki_com_tts.apkg` usando os WAV já produzidos na etapa 14;
- gera `materials_final.zip` com os arquivos finais.

## Dependências

Foram restauradas as dependências de renderização da implementação antiga:

- `reportlab>=4.2,<5`
- `genanki==0.13.1`

Ao usar uma pasta/venv antiga, execute `install.bat` ou `install.sh` para instalar as novas dependências.
