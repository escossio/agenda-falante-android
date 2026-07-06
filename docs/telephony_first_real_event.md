# Telephony First Real Event

## Objetivo

Esta etapa adiciona a primeira origem real de eventos telefônicos ao Agenda Falante Android Platform.

O objetivo é detectar chamadas recebidas e convertê-las em `IncomingCallEvent` sem acoplar a origem telephony ao Playback, ao Composer, à UI de produto ou a qualquer lógica de atendimento.

## Arquitetura

O fluxo fica assim:

`TelephonyManager -> IncomingCallMonitor -> TelephonyBridge -> IncomingCallEvent`

O monitor é responsável por observar o estado da chamada.
O bridge é responsável por traduzir o estado Android em evento da plataforma.
A UI apenas exibe status e o último evento recebido.

## Responsabilidades

### IncomingCallMonitor

- observar mudanças de estado da chamada
- detectar `CALL_STATE_RINGING`
- depender somente das APIs oficiais do Android
- exigir `READ_PHONE_STATE`
- não produzir playback
- não conhecer Experience Package, Composer ou UI

### TelephonyBridge

- converter o estado Android em `IncomingCallEvent`
- publicar o evento na plataforma
- manter a lógica de produto fora da camada telephony

### MainActivity

- exibir o status do monitor
- solicitar permissão em runtime quando necessário
- mostrar o último evento telephony

## Limitações

- O contato da chamada permanece como `Unknown` se isso exigir permissões adicionais.
- Não há atendimento automático.
- Não há reprodução automática de áudio.
- Não há acessibilidade, NotificationListener nem foreground service.
- O monitor só cobre a primeira integração real de chamadas recebidas.

## Monitoramento vs Reprodução

Monitoramento significa observar a chamada e transformar o estado em evento.
Reprodução significa tocar áudio depois que outro componente do pipeline decidir fazê-lo.

Nesta etapa, apenas o monitoramento existe.

## Resultado Esperado

Quando o telefone tocar, a plataforma recebe um `IncomingCallEvent` e atualiza a UI com o último evento, mantendo a arquitetura orientada a eventos e desacoplada do playback.
