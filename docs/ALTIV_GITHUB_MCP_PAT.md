# ALTIV DEV AI — conectar GitHub MCP com token pessoal (PAT)

## Objetivo
Na versão Android ALTIV, em **Configurações → Integrações MCP → GitHub**, conectar apenas colando um token. Não solicitar Client ID, Client Secret, configuração manual de OAuth ou JSON.

## Fluxo
1. Criar fine-grained PAT em https://github.com/settings/personal-access-tokens/new.
2. Selecionar proprietário e **Only select repositories**, escolhendo apenas os repositórios que o agente pode usar.
3. Começar com permissões mínimas: Metadata (leitura automática), Contents (Read) e Pull requests (Read) quando necessário. Para escrever código/PRs, conceder depois permissões específicas (Contents Write / Pull requests Write) e manter fluxo PR → revisão humana → merge.
4. No ALTIV DEV AI, tocar GitHub (na lista de servidores ou nas conexões rápidas), colar o token, tocar **Testar token e conectar**.
5. O app verifica a credencial em `https://api.github.com/user` por HTTPS. Isso comprova autenticação, **não** quais repositórios estão disponíveis.
6. Se validada, salva em `EnvVarRepository` (EncryptedSharedPreferences) como `ALTIV_GITHUB_MCP_TOKEN`. A entrada MCP guarda **somente** o cabeçalho de referência `Authorization: Bearer $$ALTIV_GITHUB_MCP_TOKEN`, nunca o valor.
7. O servidor MCP permanece `https://api.githubcopilot.com/mcp/`, método HTTP. Os repositórios e operações realmente acessíveis continuam dependendo do token e das políticas do GitHub.

O servidor Github MCP remoto é documentado em https://github.com/github/github-mcp-server/blob/main/docs/remote-server.md.

## Garantias e limites
- O token não é commitado, não fica em `servers.json` e não aparece no campo de edição após salvar. O armazenamento criptografado protege o segredo **em repouso**, mas as ferramentas MCP autorizadas e o ambiente Linux podem usar a credencial durante a execução. Não conceder ao agente mais privilégios que os necessários.
- Testar token não testa o acesso a todos os repositórios selecionados; a homologação exige listar um repositório permitido e tentar acessar outro não selecionado, verificando a recusa.
- Ao desconectar, o segredo local é apagado e a autenticação OAuth anterior também é purgada, inclusive a ponte do processo Linux. Para revogar o PAT completamente, revogá-lo também na conta GitHub.
- Uma configuração existente denominada `github` apontando para outra URL não é sobrescrita silenciosamente.
- O aplicativo original (flavor Github) mantém seu fluxo de OAuth/manual, não alterado.
- Identificadores técnicos legados (`com.openminis.app`, `/var/minis`, `minis-mcp-cli`) permanecem **internos** por compatibilidade com o PRoot, backups e terminal. Avisos de copyright/licença GPL-3.0 e o código-fonte correspondente devem ser preservados; não é permitido apagar atribuição legal para ocultar a origem.
- O atalho de política de privacidade do projeto upstream não é apresentado na versão ALTIV. **Antes de vender o APK**, publicar e vincular uma política de privacidade ALTIV própria, cobrindo tokens, logs, uso de IA, backups e dados locais. Não tratar a ausência do link como homologação de privacidade.

## Critérios de aceite no Samsung
- Abrir `github` mostra apenas campo de token (sem OAuth manual).
- Token restrito a um repositório funciona nesse repositório e não acessa repositórios fora do PAT.
- Token inválido gera erro e não salva o servidor como conectado.
- Abrir novamente não mostra o token em texto claro; exportar JSON MCP mostra apenas `$$ALTIV_GITHUB_MCP_TOKEN`.
- Atualizar/revogar o token, testar on/off, remoção, retorno ao chat, fechamento/reabertura do aplicativo.
- Garantir que Notion, Vercel, Supabase, localhost, chaves de IA e branding ALTIV continuam funcionando.
- Compilar, executar testes Kotlin e confirmar assinatura/instalação no aparelho antes de merge e release.


## Verificação de repositórios no painel (nova implementação)

Ao tocar **Testar token, listar repositórios e conectar**, o ALTIV valida
`GET https://api.github.com/user` e consulta, com paginação,
`GET https://api.github.com/user/repos?per_page=100&type=all&sort=full_name&page=N`.
O endpoint de repositórios requer **Metadata: Read** para PAT fine-grained.
A tela permanece aberta para exibir a conta, a quantidade de repositórios
visíveis pelo token e os primeiros 100 nomes. A quantidade pode ser parcial
quando há mais de 1.000 repositórios, caso em que a tela sinaliza isso.

- Se a chave estiver salva, revalida a lista ao reabrir a tela; um token armazenado
  ou um toggle ligado sozinho não significa que os repositórios estejam acessíveis.
- Falha 401/403, token revogado ou consulta de repositórios negada **não** confirma
  a conexão e mostra uma mensagem sem expor a credencial.
- Token válido com zero repositórios é mostrado explicitamente (não é tratado
  como acesso comprovado a repositórios).
- A lista fica em memória da tela, não grava nomes no JSON MCP e nunca grava o PAT
  em texto claro.
- **Diferença importante:** listar repositórios remotos pela API não cria
  um clone Git em `/var/minis/workspace`. Um comando `git remote -v` nessa
  pasta pode continuar vazio enquanto o MCP funciona. Para trabalhar com
  arquivos locais, o usuário deve escolher e clonar um repositório explicitamente.
- A lista usa a API REST do GitHub, portanto não substitui o teste da sessão
  MCP, nem garante que a ferramenta chamada pelo agente esteja operando.
  Após conectar, abrir uma conversa nova e solicitar ao agente que liste
  os repositórios **usando o GitHub MCP**, não apenas o shell local.

### Regressão

Rodar `AltivGitHubPatConfigTest` e `AltivGitHubRepositoryAccessTest` na CI ARM64.
Em aparelho, testar token válido, token revogado, token com zero repositórios,
com várias páginas, desativação do servidor, abertura da tela com token salvo,
e preservação dos demais servidores (Vercel, Notion e Supabase).
