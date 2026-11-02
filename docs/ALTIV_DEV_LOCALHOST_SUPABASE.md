# ALTIV DEV AI — prévia localhost e estoque Supabase

Escopo da branch `feat/altiv-localhost-preview-20261102`: adicionar **atalho funcional de prévia localhost** ao terminal Android existente e especificar a próxima integração de estoque, sem alterar banco ou credenciais de usuários.

## Auditoria da base

O OpenMinis já possui shell Alpine Linux/PRoot e sessão de terminal no Android, navegador WebView, folha de prévia URL (`UrlPreviewSheet`) e configuração de rede permitindo HTTP para `127.0.0.1`. A nova opção usa essa infraestrutura. **Não alterar** a montagem do WebView em `BrowserWebView.kt` nem as proteções contra visualização em branco e renderer destruído.

## Prévia interativa nesta fase

1. No terminal vinculado à conversa/projeto, iniciar o servidor de desenvolvimento com bind em `127.0.0.1`. Exemplo: `npm run dev -- --host 127.0.0.1`, se o framework e dependências oferecerem esse comando.
2. Tocar no novo ícone de globo do cabeçalho do terminal.
3. Informar a porta (valor inicial `3000`, também suporta `5173`, `8080` e outras entre 1 e 65535).
4. O aplicativo abre `http://127.0.0.1:<porta>/` pela prévia URL interna já existente; o WebView recebe cliques, navegação e recarga. O shell e o servidor precisam continuar em execução.
5. Endereços arbitrários não são aceitos nesse atalho; conexões externas continuam na experiência de navegador normal.

**Ainda não implementado:** iniciar e detectar automaticamente o servidor, hot-reload garantido, inspeção de console/requests na interface, execução automática de testes end-to-end Playwright e relatórios automatizados de botões. O fato de abrir uma página no WebView não equivale a validar toda sua funcionalidade.

### Critérios de aceite do atalho

- Inserir somente a porta; construir URL de loopback sem possibilidade de URL externo, injeção ou porta inválida.
- Abrir uma página HTTP válida servida localmente sem alterar código da montagem do WebView.
- Com servidor desligado, oferecer comportamento de erro do navegador; nunca declarar o teste como aprovado.
- Preservar o terminal e as conversas após abrir/fechar a prévia.
- Executar teste Kotlin `LocalPreviewAddressTest` e confirmar em aparelho Android ARM64 antes de chamar o recurso de validado.

## Próxima fase — conectores Supabase por projeto

O ALTIV DEV AI deve oferecer **Adicionar conexão** com identificação explícita do projeto Supabase (project ref), método de autenticação apropriado, ambiente (teste/produção), estado da conexão, escopos de operações permitidas e opção de revogação. Para SQL DDL, usar ferramenta de administração somente em backend confiável, nunca uma chave secreta no APK, WebView ou código de usuário. Exibir um `diff`/plano antes de criar ou alterar tabelas e exigir aprovação humana ligada a projeto e ambiente exatos.

### Contratos de ferramentas propostos

- `supabase.inspect_schema` — somente leitura; retorna tabelas, colunas, índices e políticas RLS.
- `supabase.plan_inventory` — gera SQL revisável e testes, sem executá-los; entrada: projeto, loja, necessidade de estoque, moeda e convenções.
- `supabase.apply_approved_migration` — só no backend, após aprovação explícita do hash da migração, identificador do projeto e ambiente. Retorna evidência de execução e rollback possível.
- `supabase.inventory_product` — cria/consulta/edita produtos conforme papel do usuário e RLS; sem credencial privilegiada exposta.
- `supabase.inventory_adjustment` — lançamento transacional com quantidade anterior, delta, quantidade final, justificativa, autor, horário e idempotency key; quantidades negativas só dentro do limite de estoque permitido.
- `supabase.audit_access` — testa anon, loja A, loja B e perfis de trabalho; falhar se houver vazamento entre tenants.

### Estrutura do estoque recomendada

- `stores`: `id`, `tenant_id`, `name`, `created_at`.
- `store_members`: `store_id`, `user_id`, `role` (gerente, operador, consulta).
- `products`: `id`, `store_id`, `sku`, `name`, `price_cents`, `active`, `created_at`; `unique(store_id, sku)`.
- `stock_levels`: `store_id`, `product_id`, `quantity`, `updated_at`; restrição de quantidade >= 0 e chaves compostas para bloquear referência cruzada.
- `stock_movements`: `id`, `store_id`, `product_id`, `delta`, `reason`, `user_id`, `idempotency_key`, `created_at`; escrita controlada por operação transacional.
- Dados monetários em centavos inteiros e exclusão preferencialmente lógica de produtos com histórico.

### Segurança e testes

- Ativar RLS em **todas** as tabelas expostas e revogar grants desnecessários de `anon`.
- Políticas por operação usando `auth.uid()`, vínculo de usuário com loja e regras claras de papel.
- Não usar `user_metadata` como autoridade e não permitir `service_role` na APK.
- Evitar `SECURITY DEFINER` em esquema exposto e não usar SQL livre de agentes sem revisão. Ajuste de estoque deve ser atômico para não produzir perda de atualização.
- Testar isolamento A/B para SELECT/INSERT/UPDATE/DELETE, conflito simultâneo, estoque insuficiente, duplicidade de SKU e idempotência.
- Manter banco real totalmente inalterado nesta branch; **nenhuma migração foi executada**.

## Ordem de implementação

- PR atual: botão de prévia do localhost, validação e teste Kotlin, documento de integração.
- PR futuro: healthcheck local, lista de servidores por projeto, abrir/fechar prévia e console JS/rede.
- PR futuro: conexão Supabase OAuth/secret manager, modo somente leitura e geração de migração de estoque; aplicar migração somente em banco de desenvolvimento autorizado.
- PR futuro: e2e Playwright em runner isolado e testes reais de produtos/estoque/RLS, antes de liberar operação em ambiente de produção.

Licença: preservar GPLv3 e avisos dos componentes derivados do OpenMinis. Sem merge automático, sem gasto ou ativação de serviço pago.
