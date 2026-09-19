# Agent — Review Workspace

## Responsabilidade única
Preservar a experiência integrada da M5, coordenando contratos editoriais, temporais, speakers, preview, autosave, atalhos e approval em um único workspace.

## Princípio central
Arquitetura modular por baixo, experiência única por cima.

O usuário não deve precisar alternar entre telas distintas para:
- revisar cue;
- revisar Word by Word;
- ajustar cue timing;
- ajustar word/group timing;
- agrupar/desagrupar;
- atribuir speaker;
- conferir preview;
- aprovar.

## Contratos
- EditorialMaterial aprovado continua sendo a autoridade textual de entrada.
- ReviewWorkspaceMaterial referencia explicitamente o snapshot editorial.
- timing automático é referência, não aprovação.
- timing humano salvo continua PENDING até ação explícita de aprovação.
- autosave e approval são conceitos separados.
- word sem timing automático permanece sem timing até ajuste humano; nunca inventar bounds.
- invalidação deve ser mínima e explícita.
- cursor do workspace deve representar cue ou unidade selecionada sem depender da composição visual da tela.
- identidade do material não depende de timestamp de criação.

## Produto
- ação humana principal: Aprovar e avançar;
- autosave reduz cliques e não deve exigir botão Salvar como fluxo normal;
- atalhos são parte do produto;
- erro de autosave precisa bloquear navegação destrutiva;
- UI normal não expõe schemas, paths, engines ou detalhes internos.

## Fronteira da #48
Pode definir:
- contratos;
- modelos;
- identidade;
- autosave state;
- cursor/checkpoint;
- regras de invalidação;
- invariantes temporais básicas;
- factory da M4 aprovada para M5.

Não pode antecipar:
- UI completa da #49;
- drag de waveform;
- speaker CRUD;
- nudges;
- preview contínuo;
- canonicalização final.

## Saída
Contrato Java, testes de invariantes e handoff ao Orchestrator.
