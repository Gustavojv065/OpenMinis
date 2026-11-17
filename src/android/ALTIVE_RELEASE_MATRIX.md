# ALTIVE DEV AI — matriz de aceite antes de entregar APK

Esta lista consolida os pedidos registrados para o APK comercial. **O build verde gera apenas CANDIDATO DE TESTE; não significa “100%”, não significa validação visual ou que o aparelho recebeu a versão correta.** Não divulgar como APK final de cliente até confirmar os itens manuais e o commit exato da `main`.

## 19 verificações de release

| # | Requisito contratado | Validação automatizada | Evidência manual ainda necessária |
|---|---|---|---|
| 01 | Identidade ALTIVE sem textos visíveis “Minis” | Overlays, rótulos de ferramentas e testes de marca | Percorrer telas, ferramentas, notificações e diálogos |
| 02 | Entrada só por credencial/assinatura ALTIV ADMIN | `AltivSubscriptionPolicyTest` + contrato de ativação | Testar licença válida, inválida, expirada e revogada |
| 03 | Credenciais protegidas, sem service_role no cliente | Teste KeyStore e regras de build | Conferir restauração e ausência de segredos em logs |
| 04 | Chaves NVIDIA NIM e OpenRouter, modelos selecionáveis | Testes de provider, modelos e política de custos | Chave própria de teste, erro de rede e limites de free |
| 05 | Roteamento entre modelos sem alternar silenciosamente para pagos | Teste `AltivOnlineCostPolicyTest` | Validar roteamento e fallback real com consentimento |
| 06 | GitHub MCP e acesso a repositório existente | Testes PAT/projeto/acesso | Listar repo autorizado, branch, commit e PR em ambiente de teste |
| 07 | Supabase MCP com autorização e escopo | Testes `SupabaseMcpProjectScopeTest` e `AccountScopeTest` | Revogar/testar uma conta e preservar outras, sem expor segredos |
| 08 | Skills importáveis e escolhidas pelo tipo de tarefa | Testes de importação, roteamento | Importar e executar skills reais no APK |
| 09 | Memória isolada por projeto | Teste `AltivProjectMemoryTest` | Persistência entre conversas e isolamento A/B |
| 10 | Regras globais de erros de qualidade para diversos nichos | `AltivVisualBuilderTest` + política embutida | Criar/atualizar setores diferentes sem misturar clientes |
| 11 | Auditoria Visual QA com resultados honestos | `AltivVisualAuditTest` | Screenshots e inspeção humana/visão real |
| 12 | Imagens corretas, completas e licenciadas | DOM detecta falhas e fallback; sem aprovação semântica | Conferir assunto, duplicatas, seis cards/galeria e direitos de mídia |
| 13 | Mobile/desktop e tipografia legíveis | Inspeção DOM heurística | 360, 390, 430, 768 e 1440 px em navegadores reais |
| 14 | Acessibilidade, contraste, foco e movimento reduzido | Testes Visual QA existentes | Testar navegação, contraste AA e touch targets |
| 15 | Preview localhost verdadeiro após criar/atualizar | `LocalPreviewAddressTest` | Abrir em Android, confirmar alterações e servidor ativo |
| 16 | Formulários/links/WhatsApp/mapa honestos e funcionais | Build e QA são insuficientes para backend | Executar CTA, formulário, API real ou marcar demonstração |
| 17 | Backup/restauração de projetos, memórias, skills e configurações | Rotinas `BackupExporter` / `BackupImporter` presentes | Exportar, restaurar após reinstalação de teste e conferir arquivos |
| 18 | APK ARM64 íntegro, versão única e atualização por cima | aapt, apksigner, SHA-256 e validação PRoot/Alpine | Mesma assinatura persistente + instalação de atualização em aparelho |
| 19 | Sem PRs esquecidos, CI completa e versão final da `main` | Gate de contratos + testes Android no CI | Conferir histórico de PRs e excluir explicitamente o que foi pausado |

## Critérios de bloqueio

- Nunca declarar PASS se uma imagem essencial estiver ausente, errada, repetida indevidamente, sem direito de uso ou apenas mostrar “Indisponível”. DOM/HTTP 200/build **não** provam semântica visual.
- Nenhuma liberação com bypass de licença, mistura de memória entre empresas, credenciais vazadas, API paga ativada sem autorização, perda de backup ou projeto existente sobrescrito.
- O APK debug gerado em PR é **candidato interno**, não versão comercial homologada. O script `scripts/altiv_release_gate.py --mode candidate` verifica existência de contratos/testes e deixa claro que ainda faltam build e dispositivo.
- A aprovação `--mode release` exige `main`, hash idêntico do APK, commit exato e **14 confirmações manuais** em arquivo privado externo ao repositório. Não gravar prints sensíveis, tokens ou evidência de cliente no GitHub público.
- Antes de distribuir, conferir em GitHub se a compilação saiu da `main` atual e se os PRs pertinentes estão mesclados; não entregar build de branch isolada.
- Modelos offline: **pausados deliberadamente** por decisão do usuário. Os PRs #32/#33 permanecem fora desta release e não devem ser marcados como funcionalidade entregue.
- Migração do caminho técnico `/var/minis`: **não aplicar só com substituição textual**; precisa de migração/compatibilidade testada. Nenhum nome de marca antigo deve aparecer em UI ALTIVE.
- Não ligar deploy, Supabase produção, pagamentos ou alterações destrutivas automaticamente.

## Evidência manual — formato privado de `--mode release`

Exemplo (salvar fora do repositório; preencher cada campo com `true` só após teste real):

```json
{
  "commit_sha": "SHA_COMPLETO_DA_MAIN",
  "apk_sha256": "SHA256_DO_APK_HOMOLOGADO",
  "license_denied_access": false,
  "license_valid_access": false,
  "project_memory_isolation": false,
  "nvidia_and_openrouter_keys": false,
  "github_mcp_authorized": false,
  "supabase_scoped_access": false,
  "skills_import_and_execution": false,
  "localhost_live_preview": false,
  "mobile_visual_qa_390": false,
  "desktop_visual_qa_1440": false,
  "image_subjects_checked": false,
  "backup_restore_tested": false,
  "persistent_signing_verified": false,
  "merged_pr_inventory_reviewed": false
}
```

**Regra de entrega:** nunca afirmar que 19/19 estão aprovados apenas porque o CI concluiu. Reportar itens verificados, pendentes e bloqueadores. APK só é “final” após evidência completa.
