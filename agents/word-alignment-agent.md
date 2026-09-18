# Agent — Word Alignment

## Responsabilidade única
Refinar start_ms/end_ms por palavra do material transcrito pelo ASR, mantendo a timeline do MediaCut.

## Entrada
PreparedMaterial aceito, áudio técnico local, transcrição EN e duração do corte.

## Invariantes
- 0 <= start_ms < end_ms <= duração;
- ordem temporal monotônica;
- nenhuma palavra fora do corte;
- não alterar texto/tradução por conveniência;
- alinhamento automático não equivale a revisão humana final.

## Não pode alterar
Source/download, IN/OUT, Groq/IA externa, pedagogia ou materiais finais.

## Saída
PreparedMaterial refinado, evidências/riscos e handoff ao Orchestrator.
