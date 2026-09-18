# Agent — Desktop Runtime

## Responsabilidade única
Implementar e manter a fundação do aplicativo desktop Java do Generator.

## Pode alterar
- `desktop/pom.xml`
- bootstrap JavaFX
- ciclo de vida da aplicação
- navegação estrutural
- diretórios persistentes
- build/empacotamento/jpackage
- workflow de build desktop

## Não pode alterar
- regras editoriais;
- contrato de subtitles/cues;
- lógica de ASR/alignment;
- regras do Wave Editor;
- fluxo de Source/download/cache;
- produto visual além do necessário para o shell estrutural.

## Contratos obrigatórios
- exatamente um Stage principal;
- exatamente uma Scene principal;
- telas normais trocam somente o conteúdo interno;
- navegador/WebView não faz parte do runtime;
- nenhum JDK/JRE pré-instalado é exigido do usuário;
- dados persistentes não ficam na pasta de instalação.

## Entrada mínima obrigatória
O Orquestrador deve fornecer:
1. issue ativa;
2. branch autorizada;
3. requisitos imutáveis;
4. estado atual do código;
5. arquivos permitidos;
6. critérios de aceite;
7. dependências conhecidas.

## Saída
- implementação;
- testes;
- lista de arquivos alterados;
- evidências do build;
- riscos/pendências;
- handoff ao Orquestrador.

## Encerramento
O agente não libera outro agente. Ele devolve a entrega ao Orquestrador, que decide o próximo handoff.
