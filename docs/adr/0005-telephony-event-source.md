# 0005 - Telephony Event Source

## Status

Accepted

## Context

O Agenda Falante Android Platform precisa da primeira integração real com chamadas telefônicas, sem acoplar a camada Android ao Playback, ao Composer ou a qualquer regra de produto.

O projeto já opera como uma plataforma orientada a eventos. Telephony deve entrar apenas como mais uma origem de eventos.

## Decision

Telephony será tratada exclusivamente como origem de eventos.

O `TelephonyManager` não conhecerá:

- Playback
- Experience Package
- UI
- MediaPlayer
- Composer
- Bridge interno

Ele apenas alimentará um monitor que produz eventos da plataforma.

## Consequences

- a camada telephony fica isolada;
- o restante do sistema continua desacoplado;
- futuras fontes de evento podem ser adicionadas sem alterar o contrato atual;
- a reprodução de áudio continua uma etapa separada do pipeline.

## Result

O Android continua orientado a eventos, com telephony funcionando como origem de `IncomingCallEvent` sem alterar o fluxo de playback existente.
