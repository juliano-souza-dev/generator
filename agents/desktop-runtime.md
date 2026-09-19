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

## Experiência de produto consolidada — 2026-09-19
- O runtime ativo é Java desktop; Python antigo é apenas referência funcional.
- O Generator usa uma única Stage/Scene com navegação interna entre Home, Source, Wave, Preparação e Tradução.
- Estado de projeto é explícito e versionado. Evolução do schema precisa migrar projetos sem transformar dados válidos em retrabalho.
- Artefatos mutáveis devem ser isolados por projeto; caches compartilháveis só podem ser reutilizados com identidade compatível.
- Reabrir projeto não dispara processamento pesado automaticamente.
- Se um artefato faltar, recuar apenas até a dependência mínima necessária.
- Preparação e Tradução concluídas devem ser restauradas por snapshot validado, não por simples existência de arquivo.
- Chaves/segredos de sessão não pertencem ao estado persistido.
- Upgrade, repair e uninstall devem preservar dados persistentes fora da instalação.


## Maturidade acumulada — Issue #46
- A revisão editorial possui lifecycle explícito no ProjectState: NOT_STARTED, IN_PROGRESS, COMPLETED ou INVALID.
- O ProjectState registra a fase editorial atual: CUE_REVIEW, WORD_REVIEW ou COMPLETE.
- O snapshot editorial referencia o material persistido, sua versão, a TranslationMaterial de origem, cue atual, cursor Word by Word e timestamp de atividade.
- O arquivo editorial continua sendo a fonte dos textos, aprovações e grupos; o ProjectState é a fonte de lifecycle, dependência e ponto de retomada.
- Projeto legado pode adotar um artefato editorial válido já existente ao migrar, sem perder trabalho.
- Estado de projeto desatualizado pode ser sincronizado com um artefato editorial válido; isso não exige reprocessamento pesado.
- Se um projeto afirma possuir revisão em andamento/concluída e o artefato estiver ausente ou corrompido, marcar somente a revisão como INVALID.
- Corrupção editorial nunca deve disparar Source, Wave, Preparação ou Groq automaticamente.
- Reconstrução de revisão é ação explícita do usuário e reutiliza a TranslationMaterial válida já existente.
- Retomada prioriza cursores persistidos no ProjectState e usa os cursores editoriais locais apenas como fallback/migração.
