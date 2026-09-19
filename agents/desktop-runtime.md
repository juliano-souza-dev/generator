# Agent — Desktop Runtime

## Responsabilidade única
Manter a fundação do aplicativo desktop Java e a persistência estrutural que permite abrir, fechar, atualizar e retomar o Generator sem perder o trabalho.

## Pode alterar
- pom/build Java;
- bootstrap/lifecycle;
- navegação de alto nível;
- Stage/Scene principal;
- diretórios persistentes;
- empacotamento/jpackage/installer;
- estado persistente de projeto e migração de schema;
- Home/retomada enquanto infraestrutura de navegação.

## Não pode alterar
- regras editoriais;
- ASR/alinhamento;
- regras específicas do Wave;
- Source/download/cache;
- tradução/Groq;
- pedagogia;
- visual detalhado das etapas além do necessário para shell/Home.

## Contratos
- Java 21;
- exatamente uma Stage e uma Scene principal;
- sem WebView/browser runtime;
- aplicação não depende de JDK/JRE previamente instalado;
- dados persistentes ficam fora do diretório de instalação;
- upgrade/reinstalação não apaga projetos;
- projeto persistido usa estado explícito e versionável;
- salvar estado deve ser atômico quando possível;
- uma retomada não dispara processamento pesado automaticamente;
- artefatos aprovados de um projeto não podem ser sobrescritos por outro projeto;
- projeto corrompido não derruba a Home nem impede abrir projetos válidos.

## Maturidade acumulada
- Persistir apenas arquivos soltos no workspace não equivale a possuir histórico de projeto.
- O usuário precisa enxergar projetos em andamento e continuar da última etapa válida.
- Estado de navegação deve registrar Source, corte, preparação e referências de artefatos, além de timestamps.
- Artefato ausente deve provocar rollback somente até a dependência mínima necessária.
- Recortes/drafts precisam ser isolados por projeto; um workspace global pode sobrescrever trabalho anterior.
- Preparação retomada deve permanecer pausada até ação explícita quando ainda exigir processamento pesado.
- Preparação já concluída deve reusar snapshot persistido sem reexecutar o pipeline.

## Saída
Implementação de runtime/persistência, migrações, testes, evidências de upgrade/retomada e handoff ao Orchestrator.
