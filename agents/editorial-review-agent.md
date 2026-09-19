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
- original_en da cue é imutável;
- approved_en e PT só viram aprovados por ação humana explícita;
- editar conteúdo aprovado devolve a cue ao estado pendente até nova aprovação;
- revisão editorial nunca inventa timings;
- nenhum reprocessamento de ASR, alinhamento ou Groq deve ocorrer apenas por abrir/retomar a revisão;
- salvar cada decisão independentemente da conclusão do conjunto;
- material automático é sugestão, nunca aprovação humana;
- UI fala em linguagem de produto e não expõe schemas, paths ou engines.

## Maturidade acumulada — Issue #42
- Revisão de cues é etapa própria após Tradução.
- original_en permanece somente leitura.
- approved_en/PT são persistidos por cue.
- aprovação é explícita e uma nova edição invalida a aprovação daquela cue.
- a cue atual é persistida para retomada.
- projetos já traduzidos retomam diretamente em Review sem repetir Groq/ASR/alinhamento.
- reprodução da cue usa o recorte local já aprovado.

## Maturidade acumulada — Issue #43
- Alterar approved_en exige reconciliar words[] por sequência, não retokenizar destruindo dados válidos.
- Matching usa tokens compatíveis em ordem; words preservadas mantêm timing/confidence e metadados válidos.
- Palavra inserida ou substituída nasce sem timing e sem confidence. Nunca copiar timing da word removida.
- Palavra removida deixa de existir no material; não manter órfãos.
- Pontuação/capitalização compatível não deve destruir timings válidos.
- Grupo semântico afetado por inserção/remoção/substituição é dissolvido e volta a pendente sem inventar PT.
- Toda reconciliação registra proveniência no snapshot editorial.
- Restaurar a sugestão da Tradução restaura também a estrutura/timings automáticos confiáveis.
- O codec deve recusar timing herdado que não corresponda à tradução original.

## Fronteira atual
Pode:
- revisar cue EN/PT;
- reconciliar approved_en com words[];
- preservar ou invalidar metadados conforme compatibilidade;
- restaurar a base da Tradução;
- persistir/retomar.

Ainda não pode antecipar:
- revisão Word by Word (#44);
- agrupamento/desagrupamento semântico de produto (#45);
- timing manual;
- speakers;
- materiais finais.

## Saída
Implementação, testes de invariantes editoriais, evidências de persistência/retomada e handoff ao Orchestrator.
