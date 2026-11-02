# ALTIV AI Agent — Fase 3: APK de testes Android ARM64

## Workflow

Arquivo: `.github/workflows/altiv-android-arm64.yml`

A rotina roda em `ubuntu-24.04`, sem chaves secretas do usuário e com
`permissions: contents: read`. É iniciada quando há um pull request com
alterações relevantes de Android ou do workflow, e também poderá ser acionada
manualmente em **GitHub → Actions → ALTIV - Android ARM64 APK → Run workflow**
depois que estiver na branch padrão.

A execução só disponibiliza o APK se todas as etapas concluírem com sucesso.

Etapas:
1. Fazer checkout com submódulos, sem persistir credenciais.
2. Configurar Java 17, Go 1.25+, SDK Android 36, NDK r28c, CMake 3.22.1.
3. Compilar PRoot do repositório e validar **sem substituir** os carregadores
   Termux originais (hashes exigidos por `deps/build_proot.sh`).
4. Preparar Alpine Linux e gerar `rclone.aar` com gomobile.
5. Rodar `MCPQuickConnectCatalogTest` no Gradle.
6. Compilar a variante GitHub debug (`:app:assembleDebug`).
7. Inspecionar as entradas do ZIP do APK para garantir PRoot, Alpine e loaders.
8. Publicar o APK e o checksum SHA256 como artifact do workflow, retido por 14 dias.

### Download no GitHub pelo celular

- Abra a aba **Actions** do fork.
- Entre na execução **ALTIV - Android ARM64 APK**.
- Somente após o status verde, toque em **Artifacts** → arquivo
  `altiv-openminis-arm64-debug-<número>`.
- Baixe o ZIP, extraia `app-github-debug.apk` e, opcionalmente,
  compare com o `app-github-debug.apk.sha256`.

**Importante:** abrir um PR ou adicionar o workflow não gera APK instantaneamente.
Se o GitHub bloquear o primeiro workflow (configuração de Actions,
permissões da conta ou necessidade de aprovação), libere-o na interface do
GitHub e confira os logs. Uma build falha não produz APK válido.

### Assinatura e dados existentes

O APK inicial é **debug**, continua com identidade `com.openminis.app` e
não possui assinatura de produção ALTIV.

Cada runner do GitHub pode gerar uma **chave debug diferente**. Portanto,
não há garantia de que o APK instale como atualização sobre um OpenMinis
já instalado (oficial ou de outra execução). **Não desinstale o aplicativo
com dados pessoais sem antes produzir e verificar um backup completo**.
Para atualizações futuras sem apagar dados, a fase seguinte precisa criar
um mecanismo de assinatura **estável** e seguro, sem colocar keystore,
senhas ou chaves no repositório ou logs. Alternativamente, a identidade
ALTIV pode ser separada, após avaliar os callbacks OAuth que dependem
do `applicationId`.

### Verificações pós-instalação em dispositivo Android ARM64

1. Abrir uma conversa em português e confirmar que a interface não fica preta.
2. Abrir **Integrações MCP**; selecionar GitHub, Notion e Vercel e conferir
   preenchimento sem conexão automática.
3. Em uma conta de testes, autenticar pelo navegador, validar conexão,
   listar ferramentas (`tools/list`) e executar uma ação **somente leitura**.
4. Testar cancelamento da autorização, token expirado e desconexão.
5. Rodar comando simples no Linux interno (ex.: `pwd`) e verificar
   código de saída 0 — iniciar o shell sem executar comandos não é teste suficiente.
6. Verificar permissões do Android, logs, voz e backup/restauração.

**Um teste de build verde não demonstra que OAuth nem servidores MCP funcionam
na prática.** A integração precisa ser testada em dispositivo.

### Limites e custos

GitHub Actions com runners padrão é gratuito em repositórios **públicos**,
sujeito às políticas e limites do serviço. As dependências são baixadas de
fontes públicas; não usamos APIs pagas nem alteramos Supabase ou Vercel.

O app permanece derivado do OpenMinis sob GPLv3, com atribuições preservadas.

### Diagnóstico

- Erro de NDK: validar `ANDROID_NDK_HOME`, versão e licença.
- Erro de PRoot: conferir os arquivos dos loaders Termux e seus hashes;
  **nunca regenerar os loaders com o PRoot genérico**.
- Erro de `rclone.aar`: conferir Go 1.25+, gomobile e NDK.
- Erro `Missing sandbox APK entry`: conferir o empacotamento, não publicar.
- Erro de compilação Kotlin/Gradle: consultar o primeiro erro no job de
  compilação e corrigir em novo commit/PR antes de tentar instalar.

Referência de build do projeto: [BUILDING.md](../BUILDING.md).
