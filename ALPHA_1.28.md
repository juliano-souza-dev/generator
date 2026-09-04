# Alpha 1.28 — WbW repair recovery

- A falha `hub_final.json: Word timing fora do cue` agora traz ação **Corrigir no WbW Timing**.
- O link abre a etapa 09 diretamente na cue/unidade responsável pelo erro e mantém retorno para `/materials-final`.
- Depois de salvar a correção, o Generator volta à etapa 16 sem refazer PDFs/APKG/TTS.
- O retry seletivo grava o SHA-256 da revisão WbW que causou o erro.
- Se **Gerar somente o que deu erro** for acionado sem mudança no WbW, nenhum renderer é executado e a UI informa que a dependência ainda precisa ser corrigida.
- Se houver outros artefatos com erro, eles ainda podem ser refeitos enquanto `hub_final.json` permanece pendente.
- Nenhum clamp automático foi adicionado; o timing continua sob decisão humana.
