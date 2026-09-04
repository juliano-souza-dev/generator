# Alpha 1.5 — raw JSON import fix

Correção isolada no retorno da IA externa.

## Problema
O navegador fazia `JSON.parse` e depois `JSON.stringify` antes de enviar o arquivo. Em JavaScript, valores como `3.0` são serializados como `3`, provocando falso diff de tipo nos campos protegidos.

## Correção
- upload envia o texto JSON original;
- backend lê bytes/texto original e faz o parse;
- contrato canônico continua rígido;
- nenhum campo protegido foi flexibilizado.
