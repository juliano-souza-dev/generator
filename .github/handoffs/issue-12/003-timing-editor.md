# Handoff 003 — Issue #12 → Timing Editor Agent

## Status
READY — liberado pelo Orquestrador após aceite formal do Source Ingestion & Cache Agent.

## Issue
#12

## Branch prevista
`feat/java-desktop-restart`

## Agente owner
Timing Editor Agent

## Evidência de desbloqueio
Source Ingestion & Cache Agent aceito pelo Orquestrador na issue #12.

## Entrada esperada
Contrato congelado:
`.github/contracts/source-media.md`

O agente receberá uma instância SourceMedia contendo:
- sourceId;
- canonicalUrl;
- localPath;
- title;
- durationMs;
- fetchedAt;
- cacheHit.

O Timing Editor usa apenas `localPath` e metadados necessários. Não consulta nem baixa `canonicalUrl`.

## Requisitos de produto do Wave

### Janela
- permanece no mesmo Stage/Scene;
- Wave substitui o conteúdo central;
- não abre janela de etapa.

### Timeline
- mídia local define a timeline;
- waveform corresponde à fonte local;
- playhead corresponde ao playback real;
- seleção IN/OUT é visual e editável;
- IN e OUT nunca podem produzir intervalo inválido.

### Interação
- marcar IN/OUT pelo playhead;
- arrastar limites;
- seek pela waveform;
- zoom;
- pan/scroll quando houver zoom;
- edição fina do timing;
- feedback visual imediato.

### Atalhos a validar contra o produto
Antes do aceite final, o Orquestrador deve reconciliar os atalhos existentes com o requisito ativo. Nenhum conjunto legado deve ser tratado como definitivo apenas porque já está no código.

Estado conhecido no código antecipado:
- Space;
- Shift+Space;
- A;
- S;
- setas com passos 10ms / 100ms / 1ms.

Esse código existe na branch, mas é **não autorizado/não aceito** até este handoff ser liberado.

## Arquivo original
O original no Source Cache é imutável.
O recorte deve ser derivado e salvo fora de Source Cache.

## Contrato de saída proposto
MediaCut:
- sourceId;
- localPath;
- startMs;
- endMs;
- durationMs;
- createdAt.

### Invariantes
- localPath existe;
- sourceId corresponde ao SourceMedia;
- startMs >= 0;
- endMs <= SourceMedia.durationMs;
- startMs < endMs;
- recorte não sobrescreve a fonte original.

## Critérios de aceite
- [ ] SourceMedia local é carregado sem redownload;
- [ ] playback funciona;
- [ ] waveform representa a mídia;
- [ ] playhead sincroniza com playback;
- [ ] IN/OUT podem ser marcados e arrastados;
- [ ] zoom funciona;
- [ ] pan funciona em zoom;
- [ ] nudges respeitam limites;
- [ ] intervalos inválidos são impossíveis;
- [ ] seleção pode ser reproduzida;
- [ ] fonte inteira pode ser reproduzida;
- [ ] recorte derivado é produzido sem alterar Source Cache;
- [ ] MediaCut é persistido;
- [ ] testes automatizados passam;
- [ ] tudo continua na mesma Stage/Scene.

## Fora de escopo
- ASR;
- alinhamento de palavras;
- regras de legenda;
- render final;
- Experience Validator;
- QA;
- Milestone 2.

## Handoff de saída
Retornar ao Orquestrador:
1. arquivos alterados;
2. testes;
3. contrato MediaCut final;
4. evidência de playback/waveform/corte;
5. divergências de requisito encontradas;
6. itens que o Experience Validator deve inspecionar.


## Orchestrator review — correções obrigatórias antes do aceite

### GAP-TIMING-01 — WaveformPane sem cobertura
O domínio `TimingSelection` possui testes, mas o componente que converte coordenada X ↔ tempo, aplica zoom/pan e limita drag IN/OUT não possui teste dedicado.

O agente deve tornar a matemática da viewport testável fora do JavaFX UI thread, extraindo-a para um componente/domínio puro (ex.: `WaveViewport`) ou equivalente.

Cobertura mínima:
- x → ms no zoom 1×;
- x → ms com zoom;
- pan limitado ao início/fim;
- marcador IN não cruza OUT;
- marcador OUT não cruza IN;
- playhead/viewport permanece dentro da duração.

### GAP-TIMING-02 — contrato MediaCut precisa de teste próprio
Adicionar teste que prove:
- outputPath diferente da fonte;
- duração consistente;
- intervalo inválido rejeitado;
- arquivo de saída obrigatório.

### GAP-TIMING-03 — evidência de pipeline empacotado
O smoke `--timing-smoke` deve continuar passando no app-image e no aplicativo instalado.

O Timing Editor permanece **IN PROGRESS** até esses gaps serem resolvidos.
