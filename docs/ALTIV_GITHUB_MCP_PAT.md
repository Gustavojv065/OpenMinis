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
