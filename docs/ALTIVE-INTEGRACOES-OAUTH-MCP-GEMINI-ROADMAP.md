# ALTIVE DEV AI — Plano de integrações, skills e OAuth Google, Claude e Cloudflare
Data: 2026-11-19
Status: **proposta para a próxima versão**. Este documento não habilita integrações, não instala plugins, não altera produção e não comprova qualquer direito de uso de assinaturas pessoais.

## Objetivo e contexto

O proprietário quer que o cliente instale o ALTIVE DEV AI Android ARM64, ative a licença e possa conectar os serviços que já utiliza, preferencialmente por OAuth no navegador, com uso por linguagem natural. Solicitou também estudar autenticação Google/Gemini, além da chave API atual. Prioridades: sites e SaaS com qualidade visual, baixo custo, integração GitHub/Supabase/Vercel, Figma, skills e geração de prompts.

**Observação fundamental:** os plugins instalados na conta ChatGPT do proprietário NÃO são automaticamente instaláveis, transferíveis ou autenticáveis dentro do APK. As contas, permissões e tokens do ChatGPT não podem ser reaproveitados pelo ALTIVE sem autorização e integração própria. Plugins podem embutir skills, servidores MCP, ferramentas privadas e serviços com condições próprias.

## Situação verificável no código atual (#174, main cc683bd6)

- `ui/settings/MCPIntegrationsScreen.kt` oferece cadastro de MCP e fluxos para GitHub por PAT e conexão Supabase via navegador; isso não demonstra conexão de todos os serviços listados.
- `ui/settings/SkillsManagementScreen.kt` importa skills por URL/GitHub, texto ou arquivo; importação deve validar proveniência, licença e scripts.
- `ui/settings/AddProviderScreen.kt` permite Google Gemini apenas como `apiKey` (`availableCredentials`), enquanto o fluxo OpenAI oferece `oauth`.
- `provider/ProviderFactory.kt` encaminha Gemini ao `GeminiProvider(apiKey, model)`; `GeminiProvider.kt` monta URLs com `?key=`. Não existe atualmente fluxo Google OAuth de inferência com Gemini no APK.
- A capacidade de estabelecer uma conexão MCP não significa que o modelo conseguirá executar as ferramentas de ponta a ponta; exigir testes de permissões, tool calling, efeitos reais e logs sanitizados.

## Matriz de viabilidade

| Item mostrado no ChatGPT | Plano no ALTIVE | Requisitos/ressalvas |
| --- | --- | --- |
| GitHub | **Prioridade P0**: manter/aperfeiçoar integração, permissões mínimas e uso natural | PAT atualmente; avaliar GitHub App/OAuth oficial PKCE por cliente; PR/merge/deploy só após autorização explícita |
| Supabase | **Prioridade P0**: evoluir MCP OAuth já existente | Escopo por projeto, RLS, operações privilegiadas com confirmação |
| Vercel | **Prioridade P0**: oferecer conexão por API/MCP/OAuth quando suportada | Validar autorização do app, deploy explícito, quotas e domínio |
| Figma | **Prioridade P1**: via REST OAuth ou servidor MCP remoto oficial, conforme elegibilidade | Acesso de terceiros ao MCP pode exigir disponibilidade/waitlist; sem pressupor acesso ou capacidades de escrita |
| Notion | **Prioridade P1**: integração oficial (OAuth/API/MCP se disponível) | Permissões por página/workspace, revisões antes de escrita |
| Wix | **Prioridade P2**: APIs/integrador oficial, confirmar elegibilidade | Não prometer deploy a partir do login do plugin ChatGPT |
| MagicPath / Magnific | **Pesquisa P2**: verificar APIs/MCP/contrato oficial e autorização comercial | Plugin próprio do ChatGPT não implica API pública para terceiros |
| 3D Website Creator, UI UX Designer, Frontend Design Premium, Product Design, Template Creator, Superpowers, NVIDIA Skills, omgskills | **Prioridade P1**: mapear skills legítimas por fonte e licença, adaptar/importar via SKILL.md e dependências | Não copiar conteúdo privado, plugins proprietários ou credenciais; verificar scripts e riscos, oferecer instalador seguro |
| Prompt Perfect / gerador de prompts | **P1**: criar módulo ALTIVE Prompt Studio próprio | Usar dados fornecidos pelo cliente, URL pública/autorizada Google/Instagram, salvar briefing, gerar/editar/copiar prompt; sem scraping ilegal ou inventar dados |
| Plugin Creator, Plugin Management, GPTs to Plugin, ALTIV DEV Assistant | **Avaliar caso a caso** | Não pressupor portabilidade nem acesso ao diretório interno do ChatGPT |

## Gemini: opções de login NÃO equivalentes

1. **Entrar com Google (identidade/OAuth)**: viável para login e vinculação autorizada a um projeto/conta. Usar Credential Manager / Google Identity Services no Android, OAuth client do app, PKCE quando aplicável, escopos mínimos, consentimento, armazenamento seguro, revogação e desconexão.
2. **Inferência Gemini Developer API**: seguir a autenticação autorizada pelo endpoint e projeto; chave API continua a opção suportada pelo APK. Se houver outra modalidade oficial OAuth com escopos apropriados, implementar somente depois de validar endpoint, quotas e cobrança.
3. **Vertex AI / Google Cloud**: avaliar autenticação OAuth/ADC autorizada para projetos Cloud com IAM e faturamento específicos; não tratar isso como login pessoal Gemini nem como acesso gratuito garantido.
4. **Assinatura Google AI Pro / app Gemini**: separada da cota de API. Login Google não converte assinatura individual em créditos nem desbloqueia ilimitado no ALTIVE.
5. **Gemini CLI / Code Assist**: não reutilizar tokens OAuth, sessão pessoal nem endpoints internos/privados para um app de terceiros. As condições oficiais do Gemini CLI proíbem acessar diretamente seus serviços por software de terceiros, e o login de consumidores do Code Assist/CLI foi descontinuado em 2026.
6. **Nível gratuito**: depende da disponibilidade do modelo, região, cota e projeto; nunca trocar automaticamente para faturamento pago, e nunca indicar 'grátis' sem verificar elegibilidade. Separar 'autenticado', 'cota gratuita disponível', 'crédito promocional', 'plano pago' e 'não verificado'.

## Segurança e experiência comercial

- Tela **Conexões** com 'Conectar', 'Verificar conexão', 'Permissões', 'Desconectar', status, cotas e custo real/estimado.
- Abrir autorização no navegador oficial; nenhuma senha de conta externa solicitada no chat ou capturada pelo APK.
- Proteger credenciais com Android Keystore/armazenamento cifrado; não expor secrets em logs, screenshot, backup em texto simples ou MCP.
- Escopos mínimos e por projeto/empresa; provas de isolamento entre clientes e renovação/revogação de credenciais.
- Sempre solicitar consentimento para executar ações destrutivas, PR/merge/deploy, mudanças financeiras ou instalação de pacotes.
- Distinguir 'plugin ChatGPT instalado', 'skill de código instalada', 'MCP conectado' e 'API externa autenticada'.
- Orçamento por tarefa/tenant e modelos gratuitos elegíveis: custo previsto, limite interno com pausa antes do excesso e sem fallback pago surpresa.

## Critérios de aceite e testes

1. A tela mostra as integrações elegíveis, nunca tenta copiar os tokens de ChatGPT; lista indisponibilidade e motivo.
2. Para Figma, GitHub, Supabase, Vercel e Notion, cada conexão exigida possui fluxo oficial, teste de autorização, ferramenta de leitura e (quando autorizada) ferramenta de escrita; relatórios distinguem sucesso parcial de completo.
3. Skills são instaláveis a partir de fonte auditável com validação de licença, hashes, dependências, risco de scripts e opção de desativar/remover; não podem escapar do workspace.
4. OAuth Google é testado numa conta real com consentimento e revogação, mas não se declara acesso à API Gemini até confirmar chamadas e a forma de faturamento do projeto.
5. Não há código que reuse login Gemini CLI pessoal, cookies, tokens ChatGPT ou endpoint interno não autorizado.
6. Projeto barbearia: fluxo completo projeto novo → prompt → planejamento → criação → imagens licenciadas e pertinentes → agendamento (backend real ou demo explicitamente identificado) → build → localhost → teste mobile 390px/desktop 1440px; não inventar backend.
7. APK compila no CI Android ARM64; testes de rede, segurança, custo, falhas OAuth, limites, logout e Android físico. Não marcar como homologado antes do aparelho.

## Entrega em fases (para reduzir risco)

- **Fase 0**: inventário de plugins/skills por licença e capacidade, catálogo de conectores com disponibilidade, teste MCP e custos.
- **Fase 1**: melhorias no gerenciamento de skills e MCP (GitHub, Supabase, Vercel, Figma elegível, Notion) + revogação.
- **Fase 2**: Google Sign-In oficial para identidade/Cloud e pesquisa de modalidade de API Gemini realmente autorizada; API key como alternativa, créditos e limites claramente separados.
- **Fase 3**: Prompt Studio, roteamento de skills e verificação visual.
- **Fase 4**: testes completos, auditoria e APK homologado.

## Referências oficiais consultadas

- Google AI Plans vs API billing: https://ai.google.dev/gemini-api/docs/google-ai-plans
- Gemini Developer API pricing/free tiers: https://ai.google.dev/gemini-api/docs/pricing
- Gemini Code Assist consumer deprecation: https://developers.google.com/gemini-code-assist/docs/deprecations/code-assist-individuals
- Gemini CLI terms: https://geminicli.com/docs/resources/tos-privacy/
- Google OAuth Android / Identity: https://developers.google.com/identity/protocols/oauth2/ and https://developers.google.com/identity/openid-connect/openid-connect
- Figma MCP/REST: https://developers.figma.com/docs/figma-mcp-server/ and https://developers.figma.com/docs/rest-api/oauth-apps/
- OpenAI plugin architecture: https://developers.openai.com/plugins/concepts/plugins


## Adendo 2026-11-19 — Claude (Anthropic) E Cloudflare (não confundir)

O pedido de uma terceira IA foi interpretado como **Claude, da Anthropic**, além do ChatGPT e Gemini; foi citada também **Cloudflare**, que é uma plataforma distinta para hospedagem e Workers AI. Registrar ambas como integrações distintas, sem prometer que assinaturas pessoais serão reutilizadas como API.

### Claude (Anthropic)

- A documentação oficial de **Claude Platform OAuth (beta)** permite que um app de terceiros ofereça **"Vincular organização Claude"** com consentimento para *linked billing*, sem receber a chave de API do cliente. Não é login da assinatura pessoal Claude: requer organização na Claude Console com faturamento API direto com Anthropic, administrador/Billing e app OAuth configurado. Pode ser cobrada a organização API do próprio cliente, inclusive com teto de gastos configurado. Integração exige backend/identidade federada e validação em ambiente de teste; API key continua alternativa.
- **Não** oferecer login Claude.ai Free/Pro/Max/Ultra/Team/Enterprise como forma de gastar cota de assinatura dentro do ALTIVE. A Anthropic proíbe explicitamente que desenvolvedores de apps de terceiros encaminhem solicitações pelas credenciais de assinaturas Claude.ai. Não capturar cookies ou reaproveitar tokens do Claude Code nem contornar API/pagamento.
- Separar na interface "Claude Platform — Vincular organização (API faturada)", "Claude — Chave API" e "Assinatura pessoal Claude — não aplicável a uso da API no ALTIVE".
- Não afirmar que a conta vinculada protege dados com configurações da organização do cliente: chamadas seguem o ambiente/dados e termos da app, só faturamento é vinculado.

### Cloudflare (infraestrutura e, separadamente, Workers AI)

- Desde junho de 2026, Cloudflare oferece **clientes OAuth gerenciados pelo integrador** para apps terceiros. Para Android, usar **Authorization Code + PKCE S256** sem segredo embutido no APK; requer cadastro do app, escopos mínimos, consentimento no navegador, persistência segura, refresh/revogação e **verificação de domínio para publicar cliente OAuth** para todos os consumidores. Cliente privado serve somente à conta de origem.
- Possíveis ferramentas: projetos/Workers, Pages, DNS, R2, e Workers AI conforme escopos autorizados. Não executar deploy/alteração de DNS automaticamente sem aprovação do cliente.
- Um login Cloudflare **não concede modelos Claude, ChatGPT ou Gemini nem acesso ilimitado grátis**; Workers AI tem serviços, quotas e cobrança próprios. O uso de Workers AI depende de APIs/modelos elegíveis, limites e autorização por conta. Mostrar preço e suspender fallback pago sem consentimento.

### Priorização e aceite novos

1. P1 — estudo e protótipo OAuth **Claude Platform linked billing**. Verificar elegibilidade, aprovação, limite de gasto, uma chamada de inferência autorizada, desconexão e relatório do valor cobrado ao titular. Não comercializar como Claude Pro OAuth.
2. P1 — **Cloudflare OAuth** como conector de infraestrutura. Implementar Auth Code + PKCE e escopos por tarefa. Provar listar um recurso sem alterar nada; escrita/deploy só por confirmação. Publicação depende da verificação de domínio exigida pela Cloudflare.
3. P2 — **Workers AI** como provedor de modelos separado; selecionar endpoint/modelo com compatibilidade real com as ferramentas do agente, quotas e custo. Login OAuth de infraestrutura não é teste suficiente para declarar modelos utilizáveis.
4. Expor status "Disponível para conexão", "Em desenvolvimento", "Não suportado" e "Conta de API com cobrança"; nunca mostrar "grátis" sem avaliação das cotas. Realizar testes de segurança e permissões multiusuário antes de integrar.

**Fontes oficiais:**
- https://platform.claude.com/docs/en/manage-claude/oauth-apps/overview
- https://support.claude.com/en/articles/18467148-use-your-claude-platform-account-in-a-third-party-app
- https://code.claude.com/docs/en/legal-and-compliance
- https://developers.cloudflare.com/changelog/post/2026-06-03-public-oauth-clients/
- https://developers.cloudflare.com/fundamentals/oauth/create-an-oauth-client/
- https://developers.cloudflare.com/workers/authorization/


## Ponto de execução 2026-11-19 — próximo APK integrado

**PR #59:** adiciona no código cinco skills locais originais do ALTIVE (Prompt Studio, UX/frontend, 3D, auditoria visual, ecommerce/agendamento). Instalação automática na variante `altiv`, preservando skills do usuário. CI Android ARM64 #175 disparado. **Não** são plugins terceiros reais. Não lançar APK como "integrações completas" enquanto OAuth não for implementado e testado.

**Bloqueios que exigem registros oficiais da empresa/proprietário:**

- **Gemini/Google:** cadastrar um cliente OAuth Android do ALTIVE no Google Cloud, verificar pacote `online.altiv.devai` e SHA-1/SHA-256 da assinatura utilizada, configurar consentimento/permissões, projeto Cloud e faturamento/quota, selecionar endpoint autorizado (p.ex. Vertex AI). Separar login Google de autorização de inferência. Não reutilizar fluxo privado `cloudcode-pa.googleapis.com/v1internal` do `GeminiOAuthManager.kt` para aplicação comercial, nem client secret de aplicativo alheio.
- **Claude Platform:** cadastrar app OAuth beta na Claude Console com proprietário Admin, identidade federada do backend e service account operacional. Para contas pessoais Claude Free/Pro/Max/Ultra, **não** implementar login Claude.ai OAuth no ALTIVE, nem reaproveitar tokens do Claude Code `ClaudeOAuthManager.kt` (usa cliente OAuth e headers de aplicativo terceiro). O uso da assinatura por cliente final só pode acontecer dentro do binário Claude Code oficial, não através de um proxy/cliente pessoal do ALTIVE.
- **Figma MCP:** o endpoint remoto existe, mas somente clientes aprovados no catálogo têm acesso atualmente; ALTIVE precisa aprovação/waitlist ou fluxo REST API próprio autorizado.
- **Vercel e Notion:** presets MCP já existem, precisam testes de OAuth, permissões, ferramentas efetivas e revogação.
- **WhatsApp BARBEARIA IMPÉRIO:** demonstração abriu WhatsApp com telefone `+55 11 99999-9999` não cadastrado; precisa número real fornecido pelo dono, não obter de prints aleatórios, e teste de mensagem de agendamento antes de homologação.

**Critério final:** em ambiente de teste, login oficial Google com token do *próprio* cliente registrado, validação de acesso à API com projeto/quotas próprios, Claude Platform vinculada por organização autorizada, login/logout/refresh, skills executadas, conectores reais e APK ARM64 assinado e instalado em aparelho. Caso qualquer dependência falhe, entregar status parcial com evidências, sem chamar de APK completo.

Referências oficiais: https://code.claude.com/docs/en/legal-and-compliance ; https://platform.claude.com/docs/en/manage-claude/oauth-apps/overview ; https://developers.google.com/identity/protocols/oauth2/policies ; https://developers.figma.com/docs/figma-mcp-server/remote-server-installation/
