# Handoff 002 — Issue #12 → Source Ingestion & Cache Agent

## Status
BLOQUEADO até o Orquestrador aceitar formalmente o Handoff 001.

## Issue ativa
#12

## Branch prevista
`feat/java-desktop-restart`

## Objetivo
Substituir o SourceView de prova por Source funcional no runtime Java e implementar Source Cache.

## Requisitos de produto
1. usuário informa uma URL de fonte;
2. antes de baixar, o app consulta o Source Cache pela URL normalizada;
3. se houver fonte válida, reutiliza o arquivo original;
4. se não houver, baixa uma única vez;
5. persiste o original no Source Cache;
6. grava metadados de origem;
7. entrega uma referência local da mídia ao Timing Editor;
8. interface fala em ação/resultado, não em detalhes técnicos internos;
9. tudo continua dentro da mesma Stage/Scene.

## Diretórios
Usar a fundação fornecida por `AppDirectories.sourceCacheDir()`.

## Referência legada
O projeto Python atual contém `youtube_pipeline.py` e o fluxo Source antigo. Eles servem somente como referência funcional. Não importar runtime Python para a nova aplicação Java.

## Contrato de saída proposto
`SourceMedia`:
- sourceId;
- canonicalUrl;
- localPath;
- title;
- durationMs;
- fetchedAt;
- cacheHit.

O Timing Editor deve receber a mídia por esse contrato, nunca por uma URL remota.

## Critérios de aceite
- teste cache miss → download/persistência;
- teste cache hit → zero novo download;
- teste URL equivalente/canônica;
- teste entrada inválida;
- teste metadados corrompidos/arquivo ausente;
- UI Source só libera avanço quando existe mídia local válida;
- nenhuma nova Stage é criada;
- entrega documentada ao Orquestrador.
