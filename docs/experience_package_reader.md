# Experience Package Reader

Este leitor é o primeiro consumidor real de Experience Package no Agenda Falante Android Platform.

## Objetivo

- abrir um diretório contendo um Experience Package;
- localizar `manifest.json`, `metadata.json`, `checksums.json` e `segments/`;
- carregar os JSONs com `kotlinx.serialization`;
- validar a estrutura mínima do pacote;
- expor o pacote em memória para a UI.

## Responsabilidade

O leitor apenas entende a estrutura do pacote exportado pelo Core.
Ele não reproduz áudio, não faz sincronização, não executa Bridge completo e não contém regras de negócio.

## Limitações da primeira versão

- não valida SHA-256;
- não interpreta reprodução;
- não abre os WAVs para execução;
- não implementa comunicação com outras camadas do Android Platform;
- não faz sincronização nem persistência.

## Demonstração visual

A tela inicial do Android Platform carrega um pacote de demonstração e mostra o resultado do leitor de forma explícita.

Após acionar `Load Demo Package`, a UI exibe:

- `Package ID`
- `Version`
- `Package Type`
- quantidade de segmentos
- estado de validação
- warnings, quando existirem
- errors, quando existirem

Se a validação falhar, a tela mostra `Validation: Failed` e lista os erros retornados pelo validator.
