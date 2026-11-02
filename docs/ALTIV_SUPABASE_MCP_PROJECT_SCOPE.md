# ALTIV DEV AI — Supabase por projeto (Fase 2)

## Implementação neste PR

O menu `Configurações > Integrações MCP` passa a oferecer entrada `Supabase (por projeto)`. Antes de abrir o formulário existente de conexão, o usuário informa a **referência exata do projeto** (`project_ref`). O aplicativo produz um template MCP `https://mcp.supabase.com/mcp?project_ref=...&read_only=true&features=database%2Cdocs%2Cdebugging` e preenche o formulário do servidor remoto.

**Garantias deste atalho:** só aceita identificadores alfanuméricos minúsculos de 8–32 caracteres, não constrói URL sem `project_ref`, não permite transformar uma entrada guiada `supabase-...` em conexão de escrita quando salva/editada pelo formulário, não inicia OAuth automaticamente e não faz SQL nem cria bancos. OAuth depende de autorização do usuário pelo navegador e teste real de compatibilidade do cliente.

O formulário manual de MCP continua disponível para especialistas; ele não deve ser usado para assumir que toda conexão é read-only. O mecanismo de aprovação de ferramentas e o controle de permissões de escrita precisam de revisão independente em etapa posterior.

### Testes previstos

- Projeto válido -> URL contém `project_ref`, `read_only=true` e features limitadas.
- Injeção em ref, escopo global vazio, IDs com caracteres inválidos -> formulário guiado bloqueado.
- Tentativa de editar ID ou URL removendo a restrição read-only -> não salva.
- Usuário seleciona projeto e depois cancela -> nenhum servidor criado, sem OAuth.
- Cadastro existente com mesmo ID -> abre para edição, sem duplicar.
- App Android ARM64: autenticação no navegador, listar tabelas somente do projeto escolhido, tentar escrita e confirmar rejeição.
- Sem executar migrações nem tocar em dados de clientes antes de validação em ambiente isolado.

## Próxima fase — operações de estoque (não implementadas neste PR)

Criação de tabelas `stores`, `store_members`, `products`, `stock_levels` e `stock_movements`, com chaves compostas de loja, políticas RLS por papel, movimentação transacional e idempotência; **sempre** por migração revisável em Supabase local/staging, identificada pelo `project_ref` e aprovada pelo usuário. Não expor chave service_role no Android. Deixar toda conexão de gerenciamento de esquema separada da conexão MCP read-only, preferencialmente num backend confiável.

Documentação oficial: https://supabase.com/docs/guides/ai-tools/mcp
