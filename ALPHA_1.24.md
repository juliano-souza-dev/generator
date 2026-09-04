# Alpha 1.24 — revisão visual fiel aos materiais finais

## Escopo

A etapa 15 deixa de exibir `pdf_content` como JSON cru e passa a revisar o conteúdo em blocos visuais equivalentes aos blocos consumidos pelos renderizadores finais.

## PDFs

- `Study Workbook`: Regra central, Ciclo do método, Introdução, Módulos e Plano de repetição aparecem na mesma ordem da renderização final.
- `Guide / Answer Key`: Como usar, Recuperação ativa, Transformação e Desafio final aparecem como seções legíveis. A seção de referências informa que os cards aprovados são anexados automaticamente na geração final.
- `Transcript Original` e `Transcript Bilingual`: cues aparecem linha a linha com order, speaker, EN e PT conforme aplicável.
- `Connected Speech Lab`: cada fenômeno aparece como LAB, fala-fonte, HEAR IT AS, explicação, dica de prática, nota do aluno e drill.

## Proteção e edição

- Conteúdo pedagógico editável continua editável diretamente nos blocos.
- Qualquer edição volta o PDF para `pending`.
- Timings, cue orders, transcript derivado do canônico e dados confirmados do Connected Speech permanecem somente leitura.
- O JSON de revisão e o contrato do backend não mudam.

## Anki

- Cada item mostra uma prévia visual compatível com o template final: EN → PT, highlights, mini aula, significado, explicação e novo exemplo.
- Campos pedagógicos completos continuam editáveis abaixo da prévia, incluindo `type`, `marked` e `markedPT`.
- Aprovar/recusar e persistência continuam iguais.

## Renderização final

Nenhum renderer foi alterado. A mudança é de revisão/representação: o usuário vê os mesmos campos e a mesma ordem estrutural que serão usados para gerar PDF/APKG na etapa 16.
