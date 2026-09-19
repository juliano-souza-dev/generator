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
- UI não expõe tecnologia interna;
- o ASR validado é o material base canônico da Preparação;
- tokens técnicos do motor não pertencem ao contrato de produto.

## Maturidade acumulada
- Saídas reais podem ultrapassar poucos milissegundos o fim do recorte por granularidade interna.
- Overshoot residual pequeno e explicitamente limitado pode ser normalizado para durationMs; desvios materiais continuam sendo erro.
- Tokens técnicos zero-length no limite final devem ser ignorados.
- A transcrição ASR validada deve permanecer utilizável mesmo se o refinamento temporal posterior falhar.

## Não pode alterar
Source/download, IN/OUT aprovado, tradução PT/Groq, pedagogia, materiais finais ou alinhamento refinado.

## Saída
PreparedMaterial inicial válido, evidências e handoff ao Orchestrator.

## Experiência de produto consolidada — 2026-09-19
- ASR validado é material base canônico e suficiente para a continuidade da Preparação.
- Timings de borda exigem normalização controlada: pequeno overshoot residual pode ser clampado; desvio material continua erro.
- Tokens técnicos, marcadores internos e itens zero-length não pertencem ao contrato de produto.
- Snapshot só é publicado como reutilizável depois de validação completa.
- Em retomada, ASR válido e compatível deve ser reutilizado sem transcrever novamente por padrão.
- Feedback visível usa linguagem de produto como “Transcrevendo material…” e “Transcrição concluída.”.
