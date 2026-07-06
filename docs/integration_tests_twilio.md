# Integração Twilio

## Objetivo

Esta estrutura prepara testes de integração reais para validar chamadas recebidas no celular físico com apoio de um provedor externo, começando por Twilio.

O fluxo pretendido é:

1. build do APK;
2. instalação no celular;
3. captura de `logcat`;
4. disparo da chamada real;
5. coleta de evidências em relatório local.

## Pré-requisitos

- conta Twilio ativa;
- número Twilio habilitado para voz;
- celular Android físico conectado ao AGT;
- `adb` disponível no ambiente;
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

## Como conectar o celular físico

1. habilite as opções de desenvolvedor;
2. habilite depuração USB;
3. conecte o aparelho por cabo;
4. confirme a autorização de depuração no aparelho;
5. verifique com `adb devices` que há um device físico em estado `device`.

O cenário recusa execução quando só houver emulador.

## Como executar o cenário

Execute:

```bash
integration-tests/scenarios/incoming_call_twilio.sh
```

O script:

1. compila o APK debug;
2. instala no aparelho físico;
3. abre o app;
4. inicia `logcat`;
5. dispara a chamada via Twilio;
6. aguarda a janela configurada;
7. coleta screenshot e resumo.

Para alterar o tempo de espera:

```bash
TIMEOUT_SECONDS=60 integration-tests/scenarios/incoming_call_twilio.sh
```

## Relatórios

Os relatórios ficam em:

```text
integration-tests/reports/incoming-call-YYYYmmdd-HHMMSS/
```

Artefatos esperados:

- `logcat.txt`
- `screenshot.png`
- `summary.txt`
- `twilio-call.txt`

## Cuidados com custo

- cada chamada Twilio pode gerar custo;
- teste em aparelho físico só quando necessário;
- mantenha a janela de espera o menor tempo útil;
- evite repetição desnecessária de chamadas.

## Cuidados com segredos

- não commitar `twilio.env`;
- não imprimir `TWILIO_AUTH_TOKEN`;
- não colocar segredos em docs, logs ou relatórios;
- usar apenas o exemplo sem valores reais como base.
