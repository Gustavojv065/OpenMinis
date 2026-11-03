# ALTIV DEV AI — APK independente para testes

## Motivo
O APK GitHub debug anterior tinha applicationId `com.openminis.app`, o mesmo do Minis instalado. Android rejeita substituir aplicativo assinado com outra chave, exibindo a mensagem genérica **App não instalado**. Desinstalar o Minis original colocaria dados locais em risco.

## Correção
Nova variante Android `altivDebug` com:
- `applicationId = online.altiv.devai` (isolado do app original);
- `versionNameSuffix = -altiv-test`;
- nome de aplicativo `ALTIV DEV AI` em português e padrão;
- três ícones adaptive próprios em `src/altiv/res/mipmap-anydpi-v26`, usando a identidade ALTIV (A, IA e terminal), inclusive ícone monocromático;
- sem verificação ou instalação de atualizações do Minis no app ALTIV;
- pacote e recursos da variante OpenMinis/GitHub preservados, `namespace = com.openminis.app` mantido para não quebrar classes existentes.

No GitHub Actions: compilar `:app:assembleAltivDebug`, validar com `aapt dump badging` que o pacote é `online.altiv.devai` e o nome é `ALTIV DEV AI`, executar testes de localhost, MCP, Supabase na variante ALTIV, verificar arquivo Linux PRoot e Alpine dentro do APK e disponibilizar o artifact `altiv-dev-ai-independent-arm64-N`.

## Instalação e limites
- Baixar **o APK da compilação desta branch**, não o APK `altiv-openminis-arm64-debug-6` anterior.
- **Não desinstalar o Minis existente**. O APK ALTIV deve aparecer como aplicativo separado.
- A nova variante terá armazenamento de app separado, não importará automaticamente conversas ou credenciais do Minis.
- Assinatura é `debug` gerada pelo runner CI; **não é estável entre compilações** e uma nova APK de teste pode exigir reinstalação do ALTIV (preserve dados com backup). Implementar assinatura estável por segredos protegidos antes de distribuir atualizações permanentes.
- `minis://` e certos redirects OAuth por localhost no código de origem ainda podem provocar um seletor de aplicativos quando as duas variantes estiverem instaladas; validar antes de ativar OAuth em produção. Para uso inicial, preferir APIs com chave e serviços autorizados.
- Branding externo do apk não substitui o motor de IA: funcionalidades somente estarão disponíveis quando credenciais e MCP reais forem conectados e testados.
- O código continua derivado de OpenMinis e sujeito à GPLv3, preservando atribuição e código-fonte correspondente.

## QA obrigatório
1. CI verde com `app-altiv-debug.apk` e SHA-256 publicado.
2. Conferir pacote via `aapt` com o ID distinto e rótulo ALTIV.
3. Instalar lado a lado no Android ARM64 sem remover o Minis.
4. Abrir chat e terminal, executar `pwd`, `echo ok`, iniciar servidor local e abrir porta 3000 em prévia.
5. Testar botões de navegação e atalho localhost; verificar ausência de tela preta.
6. Testar conexão Supabase **somente leitura** com projeto de testes, cancelar OAuth sem gravação indesejada.
7. Validar compatibilidade de assinatura para atualizações futuras separadamente.

**NÃO fazer merge se build falhar. NÃO alterar dados reais do Supabase.**

## Integração Supabase com um toque (atualização)

A tela **Integrações MCP** agora coloca o Supabase no topo como uma integração de desenvolvimento, com estado local `Autorizado neste aparelho` ou `Aguardando autorização` para cada projeto. Isso identifica a existência de tokens salvos, **não garante que a API remota esteja acessível neste momento**.

1. Toque em **Supabase**.
2. No painel `https://supabase.com/dashboard/projects`, abra o projeto que deseja usar, copie sua URL completa ou o ID do projeto.
3. Cole o valor no aplicativo.
4. Toque em **Entrar com Supabase**, faça login e autorize no navegador.

O aplicativo extrai o `project_ref` apenas de links HTTPS do domínio exato `supabase.com`, constrói ele mesmo a URL do MCP com `read_only=true` e grupos `database,docs,debugging`, e usa o cliente OAuth existente no Minis. Um login cancelado pode deixar um servidor **pendente** sem autorização, porque o fluxo MCP persiste os dados não secretos antes de abrir o navegador; o usuário pode tentar autenticar novamente sem cadastrar JSON nem cabeçalhos.

Essa atualização não habilita escrita no banco, não lista automaticamente projetos em outras organizações e não faz migrations. O Supabase MCP restringe `list_projects` quando o servidor está com `project_ref`. A etapa futura de criação de estoque precisa de fluxo separado com aprovação explícita do banco, RLS e plano de alteração.

**Validação obrigatória no dispositivo:** conceder OAuth no navegador e pedir ao agente para listar tabelas fictícias em um projeto de testes; se o login falhar ou ferramentas não aparecerem, registrar erro sem inserir secret no APK.
