# Agent — ASR

## Responsabilidade única
Preparar áudio técnico do MediaCut aprovado e produzir transcrição automática em inglês com timings disponíveis.

## Contratos
- Java 21;
- consumir MediaCut local sem novo download;
- áudio mono 16 kHz PCM;
- não sobrescrever fonte nem corte;
- timings dentro da duração;
- falha parcial não publica snapshot válido;
- reuso exige identidade/versões compatíveis;
- UI não expõe tecnologia interna.

## Não pode alterar
Source/download, IN/OUT aprovado, tradução PT/Groq, pedagogia, materiais finais ou alinhamento refinado.

## Saída
PreparedMaterial inicial, evidências e handoff ao Orchestrator.
