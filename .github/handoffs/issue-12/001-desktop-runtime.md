# Handoff 001 — Issue #12 → Desktop Runtime Agent

## Origem
Orquestrador

## Issue ativa
#12 — Converter o Generator em aplicativo desktop instalável

## Branch autorizada
`feat/java-desktop-restart`

## Contexto de produto
A tentativa Python/WebView foi rejeitada e o PR #14 foi fechado sem merge.
A nova implementação é Java nativo com JavaFX.

## Contratos imutáveis
- 1 Stage principal;
- 1 Scene principal;
- navegação substitui conteúdo central;
- nenhuma etapa normal abre nova janela;
- sem navegador/WebView;
- aplicação autocontida, sem exigir Java instalado;
- dados persistentes fora da instalação;
- não iniciar qualquer issue da Milestone 2.

## Stack autorizada
- JDK 21;
- JavaFX 21.0.11;
- Maven;
- jpackage.

## Estado recebido
A branch já contém:
- launcher Java;
- App JavaFX;
- NavigationState/NavigationController;
- AppShell;
- SourceView de prova;
- WaveView de prova;
- AppDirectories;
- testes de navegação/diretórios;
- workflow Windows.

GitHub Actions run 35363620893 concluiu com sucesso:
- Maven/testes;
- app-image;
- smoke test do runtime autocontido;
- jpackage;
- upload do instalador.

## Critérios desta etapa
- [x] projeto Java compila;
- [x] runtime é autocontido;
- [x] shell usa uma única Stage/Scene;
- [x] troca Source/Wave por conteúdo interno;
- [x] diretórios persistentes preparados;
- [x] instalador Windows gerado;
- [x] testes automatizados da fundação aprovados.

## Limite
Não implementar Source real/cache nem Wave real. Esses domínios pertencem aos próximos agentes dentro da mesma #12.

## Saída esperada
Relatório ao Orquestrador confirmando que a fundação está apta a receber o Source Agent.
