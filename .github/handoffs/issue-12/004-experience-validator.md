# Handoff 004 — Issue #12 → Experience Validator

## Status
BLOQUEADO até aceite formal do Timing Editor Agent.

## Objetivo
Validar a experiência integrada Desktop Runtime + Source + Wave.

## Requisitos imutáveis
- uma única Stage;
- uma única Scene;
- etapas normais não abrem novas janelas;
- Source só avança com mídia local válida;
- cache hit deve parecer reutilização rápida, sem exigir conhecimento técnico;
- Wave trabalha com a mídia local entregue pelo Source;
- original do Source Cache permanece intacto;
- recorte é derivado;
- erros devem ser apresentados em linguagem de produto.

## Cenários obrigatórios
1. primeira abertura;
2. URL inválida;
3. Source cache miss;
4. Source cache hit;
5. avanço Source → Wave;
6. retorno Wave → Source;
7. playback;
8. marcação IN/OUT;
9. drag;
10. zoom;
11. pan;
12. nudge;
13. playback da seleção;
14. salvar recorte;
15. reabrir a mesma fonte;
16. confirmar ausência de janela adicional.

## Estado de código antecipado
Pode existir código implementado antes deste handoff. Isso não constitui aceite.
O Validator avalia comportamento, não histórico de commits.

## Saída
- PASS/FAIL;
- gaps com passos reproduzíveis;
- indicação do agente owner de cada gap;
- nenhuma alteração de feature.
