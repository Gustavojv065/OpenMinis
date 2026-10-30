# ALTIV AI Agent — Fase 2: conexões MCP guiadas

## Escopo desta entrega

A tela **Configurações → Integrações MCP** agora oferece três atalhos.
Ao tocar, o aplicativo **preenche o formulário existente**, sem instalar,
executar, salvar ou autorizar nada automaticamente:

| Serviço | Endpoint público | Referência |
| --- | --- | --- |
| GitHub | `https://api.githubcopilot.com/mcp/` | [GitHub MCP oficial](https://github.com/github/github-mcp-server) |
| Notion | `https://mcp.notion.com/mcp` | [Notion MCP](https://www.notion.com/help/notion-mcp) |
| Vercel | `https://mcp.vercel.com` | [Vercel MCP](https://vercel.com/docs/agent-resources/vercel-mcp) |

O nome e o endereço são preenchidos; o método de autenticação inicial é
**entrar pelo navegador**, quando suportado pelo serviço. O usuário pode revisar
e ajustar os campos, salvar o servidor, autenticar-se e usar a integração após
confirmar a disponibilização das ferramentas.

**O botão não é sinônimo de conexão estabelecida.** Nem todos os serviços
disponibilizam autenticação compatível com qualquer cliente MCP; o GitHub
pode exigir configuração manual de token com permissões mínimas, por exemplo.
O aplicativo mantém o formulário manual e a importação JSON.

Se o ID do serviço já estiver cadastrado, o atalho abre a configuração
existente; não substitui URL nem credenciais.

## Serviços não pré-configurados

- **Figma**: a documentação limita clientes MCP remotos não listados.
  Consulte [a política de clientes](https://developers.figma.com/docs/figma-mcp-server/remote-server-installation/)
  antes de integrar um aplicativo novo.
- **Supabase**: a configuração sem `project_ref` pode dar acesso a todos os
  projetos do usuário. Antes de oferecer um atalho, criar um fluxo que exija
  projeto específico, `read_only=true` e confirmação de escopo.
  Veja [Supabase MCP](https://supabase.com/docs/guides/ai-tools/mcp).

## Validação necessária antes do merge

1. Abrir Integrações MCP com lista vazia e com servidores já cadastrados.
2. Tocar GitHub/Notion/Vercel e verificar nome/URL preenchidos, com campos editáveis.
3. Cancelar sem salvar e verificar que não foi criado um servidor.
4. Tocar um atalho já cadastrado e verificar que abre a configuração atual.
5. Testar login real pelo navegador, reconexão, revogação e ferramentas
   com contas de teste em um dispositivo Android ARM64.
6. Confirmar que os recursos de `values` e `values-pt-rBR` compilaram,
   incluindo textos com parâmetros formatados.

O catálogo tem testes unitários (`MCPQuickConnectCatalogTest`).
A compilação Android precisa de SDK/NDK, PRoot, rootfs e rclone conforme
`BUILDING.md`. Até os testes em dispositivo, **não anunciar que as
integrações já foram conectadas ou que um APK foi gerado**.

## Segurança

Não incluir tokens em URL, código, logs ou screenshots. Usar escopos mínimos,
preferir leitura e exigir autorização para operações destrutivas. O app
continua licenciado sob GPLv3 e esta mudança não altera credenciais,
permissões Android, nem execução do agente.
