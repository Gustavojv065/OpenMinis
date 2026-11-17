# ALTIVE DEV AI — validação pós-merge do PR #45

Este registro acompanha a compilação ARM64 de homologação após o merge do PR #45 na branch do PR #44.

## Escopo confirmado

- Identidade ALTIVE (PR #44) em strings visíveis ao cliente, com identificadores internos preservados para compatibilidade de backup.
- ALTIVE Smart Memory v1 (PR #45): leitura e gravação por `project_key`, isolamento entre projetos, limites de memória e testes.
- Visual QA 2.0 integrado anteriormente (PR #43).

## Observações

- Não declara que o PR #44 foi mesclado na branch principal.
- Não substitui testes físicos no Android.
- Esta documentação não muda código de produção, chaves, assinatura ou banco de dados.
- Atualização por cima depende de assinatura Android consistente.
