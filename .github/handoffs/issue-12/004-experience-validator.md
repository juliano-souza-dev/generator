# Handoff 004 — Issue #12 → Experience Validator

## Status
READY — liberado pelo Orquestrador após aceite formal do Timing Editor Agent.

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
