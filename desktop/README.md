# ImmersionHub Generator Desktop

Reimplementação Java nativa da issue #12.

## Contratos

- JDK 21.
- JavaFX 21.0.11.
- Uma única Stage.
- Uma única Scene.
- A navegação troca apenas o conteúdo central.
- Nenhuma tela comum cria nova janela.
- Dados persistentes ficam fora da instalação.

## Primeiro teste

A primeira build contém Source e Wave apenas para validar a fundação:

1. abrir Source;
2. clicar em Continuar para Wave;
3. confirmar que a mesma janela permanece;
4. voltar para Source;
5. confirmar novamente que não foi criada outra janela.

O restante do fluxo será portado dentro da própria issue #12.
