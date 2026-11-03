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
