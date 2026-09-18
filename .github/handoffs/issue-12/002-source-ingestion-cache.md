# Handoff 002 — Issue #12 → Source Ingestion & Cache Agent

## Status
READY — liberado pelo Orquestrador após aceite do Handoff 001.

## Issue ativa
#12 — Converter o Generator em aplicativo desktop instalável

## Branch autorizada
`feat/java-desktop-restart`

## Agente owner
Source Ingestion & Cache Agent

## Dependência recebida
Desktop Runtime Agent concluiu a fundação:
- Java 21 + JavaFX;
- uma única Stage;
- uma única Scene;
- navegação interna;
- diretórios persistentes via `AppDirectories`;
- instalador autocontido via `jpackage`;
- build/testes do runtime aprovados no GitHub Actions run `35363620893`.

O Source Agent **não pode alterar** esse contrato estrutural.

## Objetivo
Substituir o `SourceView` de prova por Source funcional e implementar o Source Cache no runtime Java.

## Produto esperado
1. usuário cola uma URL do YouTube;
2. a URL é validada/normalizada;
3. antes de qualquer download, o cache é consultado;
4. cache hit reutiliza o arquivo local;
5. cache miss realiza exatamente um download;
6. o original é persistido em Source Cache;
7. metadados permitem reencontrar e validar a fonte;
8. o resultado é entregue ao Timing Editor como mídia local;
9. o botão de avançar só é liberado quando a mídia local está válida;
10. tudo permanece na mesma Stage/Scene.

## Informação do legado fornecida pelo Orquestrador

### `youtube_pipeline.py`
O runtime anterior usa `yt_dlp` para:
- inspecionar a URL sem baixar;
- obter metadados como id/título;
- baixar vídeo com progresso;
- evitar playlist;
- selecionar melhor combinação de vídeo+áudio;
- aplicar fallback de qualidade quando necessário.

Funções de referência:
- `inspect_video(url)`;
- `inspect_embed_support(url)`;
- `download_video(url, project_directory, progress=...)`.

### Regra que NÃO deve ser copiada cegamente
A UI web antiga exigia vídeo "incorporável" porque dependia de reprodução no navegador.
A aplicação Java não deve manter essa exigência como regra de Source se ela não for necessária para playback local.

O contrato relevante agora é:
- URL resolvível;
- fonte acessível;
- download local válido;
- mídia local reproduzível pela próxima etapa.

### UI anterior
A tela antiga:
- aceitava URL;
- fazia validação explícita;
- só liberava avanço após fonte aprovada.

Esse comportamento de gate deve ser preservado, mas sem expor detalhes técnicos ao usuário.

## Diretórios
Obrigatório usar:
- `AppDirectories.sourceCacheDir()`;
- nenhum arquivo persistente dentro da instalação.

## Contrato de saída congelado
Criar um modelo equivalente a:

```text
SourceMedia
  sourceId
  canonicalUrl
  localPath
  title
  durationMs
  fetchedAt
  cacheHit
```

### Regras
- `localPath` aponta para arquivo existente;
- `canonicalUrl` é estável para comparação de cache;
- `sourceId` deve identificar a fonte de forma determinística;
- `cacheHit=true` significa zero download nessa execução;
- Timing Editor recebe `SourceMedia`, nunca a URL remota.

## Contrato de testabilidade
O downloader deve ficar atrás de uma interface/porta para permitir testes determinísticos sem rede.

Exemplo conceitual:
```text
SourceProbe
SourceDownloader
SourceCacheRepository
```

O agente escolhe nomes finais, mas não pode acoplar UI diretamente a yt-dlp/processo externo.

## Critérios de aceite
- [ ] cache miss → download + persistência;
- [ ] cache hit → zero novo download;
- [ ] URL canônica equivalente reutiliza cache;
- [ ] URL inválida não libera avanço;
- [ ] metadado existe mas arquivo sumiu → entrada tratada como inválida;
- [ ] arquivo existe mas metadado está corrompido → recuperação segura;
- [ ] download com falha não deixa cache falso-positivo;
- [ ] SourceView apresenta progresso/resultado em linguagem de produto;
- [ ] avanço para Wave só com `SourceMedia` válido;
- [ ] nenhuma Stage adicional é criada;
- [ ] testes automatizados passam;
- [ ] saída e riscos devolvidos ao Orquestrador.

## Fora de escopo
- waveform;
- recorte IN/OUT;
- FFmpeg de corte;
- ASR;
- regras de legenda;
- qualquer issue da Milestone 2.

## Handoff de saída
Ao terminar, devolver ao Orquestrador:
1. arquivos alterados;
2. testes executados;
3. contrato final de `SourceMedia`;
4. evidência de cache hit/miss;
5. limitações conhecidas;
6. instruções necessárias para alimentar o Timing Editor Agent.
