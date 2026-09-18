# Handoff 004 — Issue #12 → Experience Validator

## Status
CHANGES REQUESTED — a estrutura e navegação foram aprovadas, mas mensagens de falha ainda podem expor detalhes internos.

## Findings

### EV-01 — Source expõe erro bruto do downloader
Owner: Source Ingestion & Cache Agent

`SourceView` exibe `failure.getMessage()`, e `YtDlpSourceDownloader` pode devolver a última linha bruta da ferramenta.

**Correção exigida:** UI deve receber somente mensagem de produto. Detalhe técnico não pode aparecer ao usuário.

### EV-02 — Wave expõe erro bruto de processamento
Owner: Timing Editor Agent

`FfmpegMediaProcessor` pode propagar stderr bruto; `WaveView` concatena a mensagem em falhas de waveform/recorte.

**Correção exigida:** Wave deve mostrar mensagens funcionais e neutras, sem nomes de executáveis, codecs, comandos, paths ou stack/details internos.

### EV-03 — Copy dos nudges precisa ser inequívoca
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
- [ ] uma única Stage/Scene;
- [ ] Source → Wave sem abrir nova janela;
- [ ] Source altera URL → mídia anterior deixa de ser válida;
- [ ] Wave só abre com SourceMedia válido;
- [ ] waveform representa a fonte real;
- [ ] IN/OUT sempre visíveis;
- [ ] drag dos marcadores é compreensível;
- [ ] Space reproduz seleção;
- [ ] Shift+Space reproduz fonte;
- [ ] A marca IN;
- [ ] S marca OUT;
- [ ] nudges 1/10/100ms;
- [ ] zoom/pan não perde IN/OUT;
- [ ] salvar recorte produz feedback claro;
- [ ] voltar para Source mantém uma única janela;
- [ ] nenhuma mensagem de UI expõe yt-dlp, FFmpeg, Maven, JavaFX ou caminhos internos desnecessários.

## Saída
Parecer ao Orquestrador. Nenhum código deve ser alterado por este agente.
