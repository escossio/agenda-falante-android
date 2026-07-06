# Integração Twilio

## Objetivo

Esta estrutura prepara testes de integração reais para validar chamadas recebidas no celular físico com apoio de um provedor externo, começando por Twilio.

O fluxo oficial agora é manual-first:

1. AGT gera o APK;
2. o APK fica em `release-local/agenda-falante-debug.apk`;
3. o usuário instala manualmente no celular;
4. o AGT dispara uma chamada real via Twilio API;
5. o usuário valida visualmente no celular.

## Pré-requisitos

- conta Twilio ativa;
- número Twilio habilitado para voz;
- `curl` disponível no ambiente;
- credenciais locais em `integration-tests/providers/twilio/twilio.env`.

## Como criar a conta Twilio

1. crie uma conta em Twilio;
2. complete a verificação inicial exigida pela plataforma;
3. habilite recursos de voz;
4. obtenha o `Account SID` e o `Auth Token`;
5. crie ou compre um número Twilio com capacidade de ligação de voz.

## Como criar o número Twilio

Use a área de números da Twilio para provisionar um número com voz habilitada. Esse número será usado como origem da chamada.

## Como configurar `twilio.env`

Crie `integration-tests/providers/twilio/twilio.env` a partir do exemplo:

```bash
cp integration-tests/providers/twilio/twilio.example.env integration-tests/providers/twilio/twilio.env
```

Preencha os campos:

- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_FROM_NUMBER`
- `AGENDA_FALANTE_TEST_PHONE_NUMBER`

Não versionar esse arquivo. Ele é ignorado pelo Git.

## Como executar o cenário

Execute:

```bash
integration-tests/scenarios/manual_incoming_call_twilio.sh
```

O script:

1. verifica a existência do APK em `release-local/agenda-falante-debug.apk`;
2. mostra o caminho do APK e o SHA-256;
3. pede confirmação de instalação manual;
4. dispara a chamada via Twilio;
5. grava um resumo simples em `integration-tests/reports/manual-incoming-call-YYYYmmdd-HHMMSS/summary.txt`.

O script não usa `adb`, não usa `logcat` e não instala nada automaticamente.

## Relatórios

Os relatórios ficam em:

```text
integration-tests/reports/manual-incoming-call-YYYYmmdd-HHMMSS/
```

Artefatos esperados:

- `summary.txt`
- `twilio-call.txt`

## Cuidados com custo

- cada chamada Twilio pode gerar custo;
- teste em aparelho físico só quando necessário;
- evite repetição desnecessária de chamadas.

## Cuidados com segredos

- não commitar `twilio.env`;
- não imprimir `TWILIO_AUTH_TOKEN`;
- não colocar segredos em docs, logs ou relatórios;
- usar apenas o exemplo sem valores reais como base.
