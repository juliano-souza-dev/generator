# Contrato de Execução Sequencial das Milestones

Este contrato é definitivo para o desenvolvimento do Generator.

## Regra imutável

Dentro de uma milestone, as issues são executadas em ordem estrita.

1. Apenas uma issue da milestone pode estar em execução por vez.
2. A próxima issue só pode ser iniciada quando a anterior estiver 100% concluída.
3. "100% concluída" exige:
   - todos os critérios de aceite satisfeitos;
   - testes e validações aplicáveis concluídos;
   - nenhuma pendência necessária à própria issue transferida silenciosamente;
   - issue fechada no GitHub com `state_reason=completed`.
4. É proibido adiantar implementação de issues futuras.
5. É proibido executar issues da mesma milestone em paralelo.
6. Novos achados podem ser registrados antecipadamente, mas não implementados antes da sua posição na sequência.
7. Se uma issue descobrir trabalho adicional indispensável à sua própria conclusão, esse trabalho permanece na issue até ser resolvido.
8. Uma milestone posterior permanece bloqueada até a milestone anterior estar integralmente concluída.

## Ordem atual

### Milestone 1
1. #12 — Converter o Generator em aplicativo desktop instalável.

### Milestone 2
A Milestone 2 permanece integralmente bloqueada por #12.

1. #4 — Retirar Dual Scene do fluxo ativo e preservar compatibilidade legada.
2. #5 — Remover frontend aposentado de Connected Speech.
3. #6 — Remover módulos e helpers Python sem consumidores.
4. #7 — Consolidar assets compartilhados e versões do frontend.
5. #8 — Validar launcher/local-first dentro da arquitetura desktop.
6. #9 — Auditar rotas, navegação e endpoints frontend/backend.
7. #10 — Revisar Wave Editor, atalhos e comportamento de timing.
8. #11 — Executar regressão completa e transformar bugs encontrados em testes.

## Aplicação aos agentes

O Orquestrador é responsável por verificar a condição de desbloqueio antes de qualquer agente receber trabalho de implementação.

Nenhum agente especializado pode iniciar trabalho de uma issue bloqueada, mesmo quando já houver contexto, código parcial, relatório antigo ou alteração experimental disponível.

Código produzido antes da adoção deste contrato e pertencente a uma issue ainda bloqueada não deve ser continuado nem integrado até a sua vez de execução.
