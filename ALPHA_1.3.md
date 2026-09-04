# Alpha 1.3 — processamento de mídia

Nova etapa comum de processamento de mídia após o Mini Wave Editor. O pipeline é neutro ao tipo de conteúdo e será reutilizado tanto por cenas quanto por música; o fluxo específico que leva Música até o corte continua fora desta Alpha.

## Fluxo

`Fonte YouTube → Configuração → Mini Wave Editor → Processar mídia`

Ao clicar em **Salvar recorte e continuar**:

1. o intervalo IN/OUT é persistido;
2. o pipeline de mídia é iniciado;
3. a interface navega para `/process`;
4. a página acompanha logs em tempo real;
5. os artefatos aparecem individualmente assim que ficam prontos.

## Pipeline desta etapa

- download do vídeo usando `yt-dlp` com as estratégias/fallbacks existentes;
- recorte do vídeo conforme IN/OUT;
- normalização do vídeo com fallback de codecs;
- extração/conversão do áudio para WAV PCM 16 kHz mono com fallbacks;
- geração do JSON técnico inicial.

**Não existe transcrição nesta etapa.** Nenhuma cue, word, WbW ou legenda é criada.

## Artefatos

Todos ficam clicáveis individualmente na página de resultados:

- `original.mp4` — vídeo baixado;
- `scene_video.mp4` — vídeo dentro do recorte;
- `scene_audio_16k_mono.wav` — áudio extraído/convertido;
- `initial_scene.json` — JSON técnico inicial, sem transcrição.

## Interface

A página `/process` contém apenas duas áreas funcionais:

- **Logs** — andamento e fallbacks do pipeline;
- **Arquivos** — artefatos disponíveis para download.
