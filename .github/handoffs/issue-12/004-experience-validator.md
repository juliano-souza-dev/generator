# Handoff 004 — Issue #12 → Experience Validator

## Status
APPROVED — revalidação concluída após correções EV-01, EV-02 e EV-03.

## Evidências
- Source bloqueia avanço ao alterar URL;
- Source usa mensagens de produto e não repassa texto bruto interno;
- Wave só recebe SourceMedia válido;
- estrutura continua em uma única Stage/Scene;
- waveform, IN/OUT, playhead, zoom/pan e atalhos estão implementados;
- mensagens de playback/waveform/recorte são funcionais;
- ajuda explicita Shift+←/→ e Alt+←/→;
- nenhum nome de yt-dlp, FFmpeg, Maven ou JavaFX aparece na UI normal;
- build completo e instalador passaram no run `35368116777`.

## Findings

### EV-01 — RESOLVED — Source não expõe erro bruto do downloader
Owner: Source Ingestion & Cache Agent

`SourceView` exibe `failure.getMessage()`, e `YtDlpSourceDownloader` pode devolver a última linha bruta da ferramenta.

**Correção exigida:** UI deve receber somente mensagem de produto. Detalhe técnico não pode aparecer ao usuário.

### EV-02 — RESOLVED — Wave não expõe erro bruto de processamento
Owner: Timing Editor Agent

`FfmpegMediaProcessor` pode propagar stderr bruto; `WaveView` concatena a mensagem em falhas de waveform/recorte.

**Correção exigida:** Wave deve mostrar mensagens funcionais e neutras, sem nomes de executáveis, codecs, comandos, paths ou stack/details internos.

### EV-03 — RESOLVED — Copy dos nudges está inequívoca
Owner: Timing Editor Agent

A ajuda atual mostra `Shift 100ms · Alt 1ms`, sem repetir que os modificadores atuam com as setas.

**Correção exigida:** explicitar `Shift+←/→` e `Alt+←/→`.

QA permanece bloqueado.

## Build sob validação
Head: `eaf29378286d734541f0707c831332511ff8dd66`
CI de referência: `35367416279`.

O Experience Validator não implementa correções. Divergências retornam ao agente owner por meio do Orquestrador.

## Entrada prevista
- runtime desktop aceito;
- Source Agent aceito;
- Timing Editor entregue;
- instalador Windows atualizado.

## Checklist
- [x] uma única Stage/Scene;
- [x] Source → Wave sem abrir nova janela;
- [x] Source altera URL → mídia anterior deixa de ser válida;
- [x] Wave só abre com SourceMedia válido;
- [x] waveform representa a fonte real;
- [x] IN/OUT sempre visíveis;
- [x] drag dos marcadores é compreensível;
- [x] Space reproduz seleção;
- [x] Shift+Space reproduz fonte;
- [x] A marca IN;
- [x] S marca OUT;
- [x] nudges 1/10/100ms;
- [x] zoom/pan não perde IN/OUT;
- [x] salvar recorte produz feedback claro;
- [x] voltar para Source mantém uma única janela;
- [x] nenhuma mensagem de UI expõe yt-dlp, FFmpeg, Maven, JavaFX ou caminhos internos desnecessários.

## Saída
Parecer ao Orquestrador. Nenhum código deve ser alterado por este agente.
