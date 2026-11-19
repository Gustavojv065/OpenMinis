# ALTIVE DEV AI — Plano de integrações, skills e autenticação Google
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
