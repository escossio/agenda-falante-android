# Android Dev Workflow

Fluxo local recomendado para desenvolver e validar o Agenda Falante Android no notebook:

1. iniciar o emulador com `dev/start_emulator.sh`;
2. buildar, instalar e abrir o app com `dev/build_install_open.sh`;
3. abrir logs com `dev/logcat_app.sh`;
4. testar `Load Demo Package` na tela inicial.

## Observações

- os scripts usam o usuário `leonardo`;
- o SDK fica em `/home/leonardo/Android/Sdk`;
- o emulador não deve ser executado como `root`;
- o projeto continua sem mudanças de arquitetura ou funcionalidades de produto.
