# Desktop

Implementação da issue #12.

O diretório `desktop/` contém somente a camada necessária porque o Generator passa a ser um aplicativo instalado.

O núcleo continua em `app.py` e a interface continua em `static/`.

## Primeiro instalador de teste

O workflow `build-desktop-installer.yml` gera um instalador Windows por usuário. O instalador:

- instala o Generator sem exigir Python;
- abre a UI dentro de uma janela desktop;
- inicia e encerra o FastAPI junto com a aplicação;
- mantém dados persistentes em `%LOCALAPPDATA%\ImmersionHub Generator`;
- valida o executável empacotado com `--healthcheck` antes de criar o Setup.
