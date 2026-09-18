# Issue #17 — 001 ASR Agent

## Entrada aceita
MediaCut aprovado, produzido pelo Timing Editor. O agente deve consumir o arquivo derivado local em outputPath; é proibido baixar novamente a mídia ou modificar sourcePath.

## Objetivo desta passagem
Construir em Java 21 a fronteira de preparação técnica do corte:
1. áudio mono 16 kHz PCM gerado localmente por FFmpeg;
2. execução local de whisper.cpp para inglês;
3. normalização do resultado em transcrição + segmentos + timings disponíveis;
4. persistência determinística fora da instalação;
5. chave de reuso baseada em sourceId + IN + OUT + versão do pipeline/modelo.

## Contratos a criar
- PreparedMaterial: identidade, MediaCut/origem, áudio técnico, transcrição, segmentos, palavras/timings, versões.
- PreparedMaterialRepository: load/save por identidade.
- AsrEngine: porta Java, sem acoplar domínio ao processo nativo.
- WhisperCppAsrEngine: adaptador local para o binário/modelo empacotado.
- MaterialPreparationService: orquestra áudio → ASR → persistência/reuso.

## Invariantes
- 0 <= start_ms < end_ms <= MediaCut.durationMs para todo segmento/palavra temporalizado.
- ordem temporal monotônica.
- output técnico é derivado; mídia fonte/corte aprovado não são sobrescritos.
- falha parcial não pode ser publicada como material preparado válido.
- cache só é reutilizado quando identidade e versões coincidem.
- logs técnicos podem citar ferramentas; UI normal não.

## Fora desta passagem
Word Alignment refinado por CTC/ONNX, tradução PT, Groq, IA externa, pedagogia e materiais finais.

## Testes mínimos antes do handoff
- identidade determinística e invalidação por corte/versão;
- normalização/validação temporal;
- persistência e reuso;
- comando FFmpeg sem novo download;
- erro do ASR não publica snapshot válido.

Quando estes gates passarem, devolver ao Orchestrator para aceite e handoff ao Word Alignment Agent.
