# Agenda Falante Android

Android Platform oficial do Agenda Falante.

Este repositório existe para entregar as experiências geradas pelo `Agenda Falante Core` em dispositivos Android.

## Relação com o Core

- O Core produz a experiência.
- O Android consome a experiência.
- O Android não deve carregar regras de negócio do Core.
- O Android não implementa Planner, Composer, TTS ou lógica de domínio do Core.

## Relação com a Escossio Foundation

O projeto segue a base conceitual da Escossio Foundation e organiza a implementação Android como uma plataforma de entrega, não como um segundo core.

## Roadmap inicial

1. Bootstrap do projeto Android.
2. Tela Compose mínima.
3. Infraestrutura de Bridge.
4. Infraestrutura de Playback.
5. Integração com Experience Package.
6. Permissões e automação necessárias.

## Estrutura conceitual

- `docs/`
- `app/`
- `bridge/`
- `platform/`
- `playback/`
- `permissions/`
- `storage/`
- `automation/`
- `ui/`
- `core-contract/`
- `tests/`

## Build local

Pré-requisitos para compilar localmente:

- JDK instalado e apontado por `JAVA_HOME`;
- Android SDK instalado;
- Android Studio para sincronização, emulação e inspeção;
- Gradle Wrapper presente no repositório.

Se o ambiente não tiver JDK ou Android SDK, o build fica pendente mesmo com a estrutura do projeto pronta.
