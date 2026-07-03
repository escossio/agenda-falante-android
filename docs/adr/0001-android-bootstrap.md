# ADR 0001: Android Bootstrap

## Contexto

O Agenda Falante precisa de uma Android Platform oficial para entregar experiências produzidas pelo Core.

## Decisão

Inicializar um projeto Android moderno com:

- Kotlin;
- Gradle Kotlin DSL;
- Jetpack Compose;
- Material 3;
- `minSdk` 26;
- `targetSdk` alinhado com a versão mais recente suportada;
- namespace `com.escossio.agendafalante`.

## Arquitetura inicial

O repositório começa com uma tela Compose mínima e uma estrutura conceitual para futuros módulos de plataforma.

## Separação entre Core e Android

O Core produz a experiência.
O Android entrega a experiência.

Nenhuma regra de negócio do Core deve ser implementada nesta base Android.

## Consequência

O bootstrap cria apenas a fundação técnica para a plataforma, sem funcionalidades de produto.

## Limitação do ambiente

Este bootstrap foi preparado em um ambiente sem JDK e sem Gradle instalados localmente, então a validação efetiva de `assembleDebug` depende de um ambiente Android completo.
