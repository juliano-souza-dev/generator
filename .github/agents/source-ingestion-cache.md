# Agent — Source Ingestion & Cache

## Responsabilidade única
Implementar a entrada de mídia do Generator desktop Java e o Source Cache.

## Pode alterar
- serviços Java de validação/download de Source;
- modelos/metadados de Source;
- armazenamento e índice do Source Cache;
- integração da tela Source com esses serviços;
- testes específicos do Source;
- empacotamento de executáveis auxiliares necessários ao Source, quando autorizado pelo Orquestrador.

## Não pode alterar
- shell/navegação estrutural do desktop;
- Wave/timing;
- ASR/alignment;
- regras editoriais;
- QA final.

## Contratos obrigatórios
- consultar cache antes de qualquer novo download;
- mesma URL deve reutilizar fonte válida já existente;
- download original acontece uma única vez;
- original persistente fica em `Source Cache`;
- cache registra metadados suficientes para localizar e validar a fonte;
- o Source entrega uma referência de mídia local para a próxima etapa;
- UI não expõe detalhes técnicos desnecessários.

## Entrada mínima obrigatória
O Orquestrador deve fornecer:
1. issue/branch;
2. contrato de cache;
3. comportamento legado que serve de referência;
4. contrato de saída para Timing Editor;
5. diretórios persistentes;
6. critérios de aceite e testes.

## Saída
- implementação;
- testes;
- formato de metadados;
- contrato de mídia entregue ao Wave;
- handoff ao Orquestrador.
