# Agent — Word by Word

## Responsabilidade única
Implementar e validar a revisão humana das unidades Word by Word sobre material editorial já revisado por cue.

## Entrada
- EditorialMaterial válido;
- todas as cues aprovadas;
- words[] reconciliadas com approved_en;
- requisito ativo da milestone.

## Regras permanentes
- Java 21 + JavaFX;
- revisão é unitária e aprovação é explícita;
- PT pode ser editado livremente;
- English da unidade só pode mudar quando a alteração não muda o token estrutural; troca real de palavra volta para revisão de cue;
- alterar item aprovado devolve somente aquele item a PENDING;
- timing automático é referência e não aprovação humana;
- word nova sem timing continua sem timing; nunca inventar bounds;
- nenhuma etapa automática anterior é refeita ao abrir ou retomar;
- progresso por cue e total deve ser derivado do EditorialMaterial persistido;
- UI fala em linguagem de produto.

## Fronteira da Issue #44
- revisar uma unidade por vez;
- contexto da cue;
- editar EN dentro da compatibilidade estrutural;
- editar PT;
- salvar/aprovar;
- anterior/próxima/próxima pendente;
- reabrir item aprovado;
- persistir cursor de retomada.

## Maturidade acumulada — Issue #45
- grupos semânticos continuam sendo as mesmas words; não criar schema paralelo de unidades;
- grupo é contíguo, dentro da mesma cue, com um LEAD e um ou mais MEMBER;
- navegação/progresso contam o grupo como uma única unidade;
- agrupar com anterior/próxima pode mesclar grupos já existentes;
- mudança de grupo invalida somente as unidades afetadas;
- cada word preserva sua tradução individual conhecida para permitir round-trip seguro;
- somente o líder contém a tradução corrente do grupo; membros mantêm PT de grupo vazio;
- desagrupar restaura traduções individuais conhecidas e deixa todas as unidades afetadas PENDING;
- tradução individual desconhecida permanece vazia; nunca inventar conteúdo;
- ordem das words e timings existentes são preservados;
- material editorial v1.0 é aceito e atualizado para v1.1 quando o primeiro agrupamento exigir o backup individual.

## Ainda não pode antecipar
- timing manual;
- speakers;
- materiais finais.

## Saída
Implementação, testes de revisão parcial/completa, persistência e handoff ao Orchestrator.
