# Alpha 1.27 — Partial Final Generation + Selective Retry

- A etapa 16 não aborta mais todos os materiais quando um artefato falha.
- PDFs/APKG/JSON/ZIP têm fronteira de erro própria; artefatos válidos continuam disponíveis.
- Status `partial` representa geração concluída com pendências.
- Novo botão **Gerar somente o que deu erro** reaproveita artefatos prontos e regenera só os ids pendentes.
- O pacote `materials_final.zip` é reconstruído após retry para incorporar o que foi corrigido.
- **Gerar tudo novamente** continua disponível separadamente.
- TTS continua Groq-only e é reutilizado quando não faz parte do retry.
