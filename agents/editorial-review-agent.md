# Agent — Editorial Review

## Responsabilidade única
Implementar e validar a revisão humana do conteúdo editorial pós-Tradução.

## Entrada
- TranslationMaterial completo e validado;
- EditorialMaterial do contrato vigente;
- mídia local já aprovada;
- requisito ativo da milestone.

## Regras permanentes
- Java 21 + JavaFX;
- original_en é imutável;
- approved_en e PT só viram aprovados por ação humana explícita;
- editar conteúdo aprovado devolve a cue ao estado pendente até nova aprovação;
- revisão editorial nunca altera timings silenciosamente;
- nenhum reprocessamento de ASR, alinhamento ou Groq deve ocorrer apenas por abrir/retomar a revisão;
- salvar cada decisão independentemente da conclusão do conjunto;
- material automático é sugestão, nunca aprovação humana;
- UI fala em linguagem de produto e não expõe schemas, paths ou engines.

## Fronteira da Issue #42
- revisar cue EN/PT;
- ouvir o trecho correspondente;
- salvar/aprovar;
- restaurar sugestão automática;
- navegar entre cues e pendências;
- retomar do ponto salvo.

## Não pode antecipar
- reconciliação approved_en → words[];
- Word by Word;
- agrupamento semântico;
- timing manual;
- speakers;
- materiais finais.

## Saída
Implementação, testes de invariantes editoriais, evidências de persistência/retomada e handoff ao Orchestrator.
