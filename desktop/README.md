# ImmersionHub Generator Desktop

Implementação Java nativa da issue #12.

## Stack

- JDK 21.
- JavaFX 21.0.11.
- Maven.
- `jpackage` para app-image e instalador Windows autocontido.
- `yt-dlp` e FFmpeg empacotados como ferramentas auxiliares verificadas por checksum no CI.

O usuário final não precisa instalar Java, Python, yt-dlp ou FFmpeg separadamente.

## Contrato de janela

A aplicação possui:

- uma única `Stage` principal;
- uma única `Scene` principal;
- navegação que substitui apenas o conteúdo central;
- nenhuma janela nova para etapas normais;
- nenhum WebView/navegador no fluxo principal.

Diálogos adicionais só podem existir quando forem realmente modais.

## Instalação x dados persistentes

No Windows per-user:

```text
Programa:
%LOCALAPPDATA%\Programs\ImmersionHub Generator\

Dados:
%LOCALAPPDATA%\ImmersionHub Generator\
├── source-cache\
├── projects\
├── workspace\
├── logs\
└── settings\
```

Atualização ou desinstalação do programa não deve apagar os dados persistentes.

## Source

A etapa Source:

1. recebe uma URL de vídeo;
2. normaliza a URL;
3. consulta o Source Cache antes de qualquer download;
4. reutiliza a mídia em cache quando válida;
5. baixa somente quando necessário;
6. persiste o original e seus metadados;
7. entrega um `SourceMedia` local para a etapa Wave.

Contrato canônico:
`.github/contracts/source-media.md`

O original em Source Cache é imutável para os consumidores.

## Wave / Timing

A Wave recebe apenas `SourceMedia` local.

Ela oferece:

- vídeo e waveform no mesmo contexto;
- seek/playhead sincronizados;
- IN/OUT arrastáveis;
- playback da seleção e da fonte inteira;
- zoom normal 1×–8×;
- Super Zoom até 64×;
- pan;
- nudges de 1/10/100 ms;
- undo/redo;
- autosave de timing por fonte;
- restauração do timing ao retornar;
- estado de salvamento visível;
- navegação por teclado;
- geração de recorte derivado via FFmpeg.

O recorte nunca sobrescreve a mídia original.

Contrato canônico:
`.github/contracts/media-cut.md`

## Workspace

Mídias derivadas e estado de edição ficam sob:

```text
%LOCALAPPDATA%\ImmersionHub Generator\workspace\
```

O Source Cache contém somente a fonte persistente e seus metadados.

## Pipeline de agentes da issue #12

A execução da issue segue o contrato sequencial:

1. Desktop Runtime Agent.
2. Source Ingestion & Cache Agent.
3. Timing Editor Agent.
4. Experience Validator.
5. QA / Review.

Um agente só é liberado após aceite formal do anterior pelo Orquestrador.

## CI / release gate

O workflow `build-java-desktop.yml` valida:

1. Maven clean/test/package;
2. app-image autocontida;
3. smoke do launcher;
4. ferramentas empacotadas;
5. waveform e corte via FFmpeg;
6. geração do instalador EXE;
7. instalação real e launcher instalado;
8. pipeline Timing no app instalado;
9. desinstalação registrada pelo Windows;
10. preservação dos dados persistentes após uninstall;
11. upload do instalador somente após os gates anteriores.

Builds antigos do mesmo PR são cancelados por `concurrency` para que o gate represente apenas o commit candidato atual.
