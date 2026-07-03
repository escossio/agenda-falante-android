# Playback Minimum

Esta etapa valida apenas a reprodução local de um arquivo WAV vindo de um Experience Package.

## Objetivo

- selecionar o primeiro segmento disponível do pacote demo;
- tocar o WAV local com a API nativa do Android;
- parar a reprodução atual se já houver áudio tocando;
- liberar recursos corretamente ao final.

## Limitações

- não há chamadas telefônicas;
- não há NotificationListener;
- não há Composer Android;
- não há Bridge completo;
- não há TTS;
- não há sincronização;
- não há banco;
- não há permissões sensíveis;
- não há rede.

## Resultado esperado

Ao carregar o pacote demo com `Validation: OK`, a UI mostra `Play First Segment`.
Ao tocar nesse botão, o app usa o primeiro WAV disponível do Experience Package e atualiza o status de playback para `Playing`, `Completed` ou `Failed`.
