# Agent — Translation / Groq

## Responsabilidade única
Implementar a etapa de tradução do Generator desktop Java após a Preparação, usando Groq como caminho principal e preservando um pacote de processamento externo sempre disponível.

## Entrada
- AlignedMaterial/PreparedMaterial aceito;
- transcrição EN;
- segmentos/timings e palavras/timings protegidos;
- áudio técnico já preparado;
- contrato de saída definido pela issue ativa.

## Regras de integração
- Java 21;
- usar API HTTP oficial compatível com o endpoint vigente da Groq;
- consultar metadados do modelo em runtime em vez de congelar context window no código;
- não hardcodar RPM/TPM/RPD/TPD como verdade da conta;
- consumir headers de rate limit devolvidos pela API;
- fila local sequencial por padrão;
- estimar orçamento antes de cada request e reservar margem para envelope/saída;
- chunking em unidades semânticas completas;
- nunca cortar uma cue/segmento silenciosamente no meio;
- retry limitado com backoff para 429/transitórios e respeito a Retry-After;
- autenticação/contrato inválido não entra em loop de retry;
- preservar IDs, ordem e campos temporais;
- resposta parcial nunca é aceita como completa;
- segredo/API key nunca aparece em log, snapshot ou artefato de projeto;
- a API key pertence ao **Generator**, não ao projeto e não à sessão;
- a configuração persistente da chave deve usar mecanismo seguro do sistema operacional;
- uma chave já configurada deve ser reutilizada por todos os projetos;
- ausência de chave ao iniciar Groq abre configuração; 401/403 deve pedir atualização da credencial.

## Structured output
Quando o modelo ativo suportar Structured Outputs, preferir JSON Schema estrito. Caso contrário, usar JSON mode/validação local equivalente. Em todos os casos a validação final pertence ao Generator.

## Fallback externo
- o pacote externo é produzido sempre, independentemente do sucesso Groq;
- contém o áudio técnico da cena e instruções completas do contrato de retorno;
- falha/parcialidade Groq mantém o pacote disponível;
- importação externa passa pela mesma validação de completude/integridade;
- Groq e IA externa convergem para o mesmo TranslationMaterial, portanto não duplicar arquivos canônicos sem necessidade real de produto.

## UI
Falar apenas em linguagem de produto, por exemplo:
- Traduzindo material…
- Material pronto.
- Algumas traduções precisam ser concluídas.
- Abrir pacote externo.
- Config.

Não expor modelo, endpoint, TPM/RPM, bibliotecas, comandos ou paths na UI normal.

## Maturidade inicial
- A documentação oficial da Groq informa que limites variam por modelo/conta e que headers de rate limit acompanham respostas; 429 pode incluir Retry-After.
- O endpoint de Models expõe context_window dos modelos ativos e deve orientar o orçamento em runtime.
- Structured Outputs com JSON Schema é preferível quando suportado, mas suporte varia por modelo.
- Limites vistos em páginas públicas são referência operacional, não constantes de produto.

## Não pode alterar
Source/download, IN/OUT, Wave, ASR/DTW, timings aprovados, pedagogia posterior, TTS ou materiais finais.

## Saída
Implementação, testes de completude/chunking/rate-limit/fallback, evidências, riscos e handoff ao Orchestrator.

## Maturidade acumulada — Issue #18
- O limite exato da conta/projeto não deve ser inferido das tabelas públicas: o Generator consulta metadados do modelo em runtime e reage aos headers efetivamente retornados.
- Os headers públicos atuais expõem especialmente RPD/TPM restantes e resets; 429 usa `retry-after`. Não inventar RPM disponível a partir desses headers.
- O orçamento de contexto deve usar `context_window` e `max_completion_tokens` do modelo ativo com margem conservadora, nunca valores congelados da documentação.
- Chunking é por cues inteiras. Uma cue nunca é cortada silenciosamente para caber.
- Nenhum chunk parcial é persistido como tradução concluída. Só o conjunto integral validado é publicado.
- O pacote externo é criado antes da tentativa Groq e permanece disponível mesmo após sucesso automático.
- O pacote externo desta etapa contém somente o áudio preparado e a instrução/contrato completo de retorno, conforme decisão de produto.
- Retorno externo e retorno Groq convergem no mesmo TranslationMaterial validado; campos de timing/texto protegido continuam sob autoridade local.
- Falha 400/401/403 não entra em loop de retry; 408/429/5xx têm retry limitado, com `retry-after` quando disponível.

## Experiência de produto consolidada — 2026-09-19
- Tradução é uma etapa própria: Preparação concluída → Tradução.
- Groq é caminho principal, mas nunca gate único. Processamento externo permanece disponível quando Groq falha, fica parcial ou atinge limites.
- O pacote externo é criado antes da tentativa Groq e continua disponível mesmo após sucesso automático.
- Nesta etapa, o pacote externo contém o áudio técnico preparado e a instrução/contrato completo de retorno JSON.
- O pacote já gerado pode ser acessado abrindo sua pasta; não criar fluxo de download/exportação sem necessidade de produto.
- Retorno Groq e retorno externo convergem no mesmo TranslationMaterial validado.
- IDs, ordem, EN aprovado, words e timings são protegidos pelo Generator; somente conteúdo autorizado da tradução pode mudar.
- Nenhum chunk parcial vira resultado final. Publicação é integral e validada.
- Contexto e rate limits vêm do modelo/headers reais; não congelar limites públicos como regra de produto.
- Retry é conservador: 408/429/5xx podem repetir com espera; 400/401/403 e violações de contrato não entram em loop.
- A chave Groq é global do Generator, persistida de forma segura fora de ProjectState, reutilizada entre projetos e reinicializações.
- Ao faltar chave, “Traduzir com Groq” deve abrir a configuração. O usuário também precisa de Config permanente para trocar a credencial quando quiser.
- UI fala em “Traduzindo material”, “Traduções prontas”, “opção externa” e “Config”; modelo, endpoint, tokens e retries ficam no log técnico.
