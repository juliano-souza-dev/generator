# Agent — Word Alignment

## Responsabilidade única
Tentar refinar start_ms/end_ms por palavra do material transcrito pelo ASR, mantendo a timeline do MediaCut.

## Entrada
PreparedMaterial aceito, áudio técnico local, transcrição EN e duração do corte.

## Invariantes
- 0 <= start_ms < end_ms <= duração;
- ordem temporal monotônica;
- nenhuma palavra fora do corte;
- não alterar texto/tradução por conveniência;
- alinhamento automático não equivale a revisão humana final;
- o refinamento nunca pode invalidar um PreparedMaterial ASR já aceito.

## Regra estrutural
O DTW é **refinamento opcional**, não gate fatal da Preparação.

- se o refinamento for válido, publicar timings refinados;
- se falhar por âncoras, divergência ou outra condição interna, preservar os timings ASR válidos e permitir continuidade;
- registrar a causa técnica da falha para diagnóstico;
- snapshot deve registrar internamente a origem dos timings;
- reabrir um snapshot de fallback não deve disparar DTW novamente automaticamente.

## Maturidade acumulada
- Saída DTW pode ter overshoot residual no fim do recorte e deve seguir a política de borda do ASR.
- Palavras consecutivas podem compartilhar a mesma âncora discreta; isso não significa regressão. Runs iguais devem ser distribuídas deterministicamente numa janela válida.
- Âncora seguinte menor que a anterior é regressão real e continua bloqueada para o refinamento.
- Não transformar particularidades experimentais do alinhador em falhas fatais do produto.

## Não pode alterar
Source/download, IN/OUT, Groq/IA externa, pedagogia ou materiais finais.

## Saída
AlignedMaterial refinado ou fallback ASR explícito, evidências/riscos e handoff ao Orchestrator.
