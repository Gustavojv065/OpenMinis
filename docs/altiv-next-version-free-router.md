# Próxima versão ALTIVE DEV AI — roteador grátis universal

**Origem:** feedback do teste mobile de 15/11/2026, tela "Escolher modelo"; usuário selecionou NVIDIA Nemotron 3.5 Super VL e pediu a opção de seleção automática *para todos os modelos conectados*, não apenas para grupos montados manualmente.

## Problema observado

- O botão "ALTIV Online First · APIs prioritárias" e "Somente :free" aparecem no topo, mas pedem um modelo OpenRouter `:free` pré-configurado.
- A estratégia de fallback depende de montar manualmente um grupo.
- Com mais de uma chave API, um usuário não técnico não sabe como selecionar gratuitamente um modelo de código, visão, ferramentas ou imagens.

## Objetivo de UX

Um **roteador automático global** com "Preferir gratuitos" e "Somente gratuitos" no seletor de modelos, para todas as instâncias de API configuradas; criar grupo manual deve ser **opcional**, não obrigatório. Mostrar o modelo escolhido e o motivo da mudança. Permitir fixar modelo e desligar automação.

## Política de custo

1. "Gratuito" deve ser verificado por catálogo/metadados do provedor (por exemplo `openrouter ...:free`), não deduzido porque um provedor oferece algum free tier; APIs com cota gratuita podem usar modelos faturáveis.
2. Nunca adicionar um modelo com preço desconhecido ao modo estrito `Somente gratuitos`.
3. Nunca usar modelo pago sem consentimento explícito do usuário. Após cota/limite esgotado, avisar e oferecer outras opções.
4. Não fazer chamadas de testes custosas em loop; manter proteção do orçamento de ferramentas do PR #38.

## Roteamento inteligente por tarefa

- **React, TypeScript e backend:** modelo habilitado com boas capacidades de código e uso de ferramentas.
- **Screenshot / Visual Builder:** modelo que aceite pixels de imagem ou Vision Group comprovadamente funcional, respeitando proteção do PR #39.
- **Geração de imagens:** requer endpoint gerador de imagens, não confundir com modelos de leitura de imagem.
- Falha 429/timeout/sem suporte: selecionar outro *modelo compatível e elegível*, sem trocar para pago silenciosamente, sem perder contexto/conversa.

## Implementação técnica sugerida

- Estender `AltivOnlineCostPolicy`, que hoje só reconhece `OpenRouter :free` como explicitamente grátis; adicionar classificação baseada em capacidades/custos oficiais atualizáveis e validação de cotas.
- Ampliar `AltivModelHealthRegistry`, que hoje reordena modelos somente de grupos explicitamente criados.
- Adicionar seleção automática por capacidade/modo de custo a partir das instâncias de provedores já autorizadas, sem criar grupos permanentes sem solicitação.
- Uma interface universal no topo do seletor, com estado claro `Automático / Preferir grátis / Somente grátis / Manual` e estado de falta de alternativas.
- Preservar a preferência de seleção e evitar falsos sucessos.

## Critérios de aceite

- Uma única API com modelo gratuito: sem configuração de grupo.
- Duas ou mais APIs com modelos grátis adequados: seleção automática conforme tarefa.
- Falhas de cota/503: fallback previsível, limitado, com feedback visual.
- Sem alternativa gratuita: informação clara, sem tentativa de API paga.
- Modelo sem visão não recebe a instrução de replicar screenshot às cegas.
- Testes unitários, integração, build Android e regressão nos fluxos de API/skills.
- Não interferir nas licenças ALTIV ou credenciais Supabase/GitHub.

## Teste relacionado: landing page de barbearia

O usuário está testando a construção de uma landing page a partir de dados e imagens públicos de uma barbearia enviados a partir de sua ficha do Google. Critérios: usar os dados fornecidos, sinalizar informação não verificada, evitar inventar endereço/horário/avaliações, pedir fotos ou acesso autorizado se o agente não puder obter imagens do perfil, construir o site, verificar responsividade e imagens. Não acessar dados privados da conta Google Business sem integração autorizada.

**Escopo:** documento de planejamento; nenhuma mudança funcional até implementação e testes na próxima versão.


## Próxima versão: eliminar a marca Minis da experiência ALTIVE

**Feedback em 15/11/2026:** o usuário pediu para deixar o produto inteiramente identificado como **ALTIVE DEV AI / Altive**, sem exibir "Minis" em telas, mensagens de erro, títulos, logs voltados ao usuário, diálogos, modelos de prompt, notificações, menus, pastas exibidas na UI ou documentação destinada a clientes.

### Escopo de correção

- Auditar todas as strings e textos exibidos no flavor `altiv` em português e inglês; corrigir referências residuais "Minis", "Agente Minis", "Minis Shell", "Minis Skills" e "Sobre o Minis" inclusive em notificações, permissões, falhas, configurações e onboarding.
- Auditar textos gerados automaticamente por ferramentas, shells, previews, templates, mensagens de erros e saída de funções que apareçam em conversas; alterar apresentação externa para ALTIVE DEV AI quando o texto for controlado pelo aplicativo.
- Aproveitar e ampliar os testes de branding existentes `AltivFullBrandingTest` e `AltivBrandingConsistencyTest` para cobrir a UI real do flavor `altiv`, sobretudo notificações, atualizações, importação de skills, backups, provedores, MCP e erros de sistema.
- Preservar a identidade do aplicativo `online.altiv.devai`, os dados e caminhos existentes, compatibilidade de backups, importação de skills, sessões, plugins, servidor local e as assinaturas Android.
- **Não executar substituição global de strings em código-fonte.** Identificadores técnicos necessários à compatibilidade (`com.openminis.app`, `/var/minis`, `minis-mcp-cli`, nomes de bibliotecas, chaves persistidas, nomes de dependências e o código upstream da outra variante) exigem migração versionada e testes antes de qualquer renomeação. Nunca quebrar Android appId ou perfil de sandbox para apenas alterar branding.
- Manter avisos de licença, atribuição e créditos de código aberto legalmente exigidos; rebranding não autoriza remover autoria/licenças upstream. Informações de terceiros podem permanecer em seção de créditos, fora da experiência principal.

### Critérios de aceite

1. Executar auditoria de screenshots, strings e fluxos Android com idioma PT-BR e inglês, modo escuro, setup, notificações e falhas, procurando a marca antiga visível.
2. Todas as superfícies **voltadas ao usuário** devem exibir ALTIVE DEV AI (ou Altive, conforme contexto), sem mensagens "Minis".
3. Reabrir banco, conversas, skills e workspaces antigos sem perda de dados e comprovar exportação/restauração.
4. Comprovar instalação por cima de APK anterior de mesma assinatura e verificar suporte a reinstalação.
5. Não alterar funcionalidades dos provedores, GitHub, Supabase, licença, fluxo de atualização nem testes existentes.
6. Conferir `src/android/app/src/main/res/values-pt-rBR/strings.xml`, que contém muitos textos upstream "Minis", e a cobertura por overrides `src/android/app/src/altiv/res`. Corrigir no flavor ALTIV sem rebrand do upstream inteiro.

## Homologação relatada pelo usuário: licenciamento ALTIV ADMIN

**Resultado comunicado em 15/11/2026:** usuário informou que gerou uma **nova chave de licença no painel ALTIV ADMIN**, digitou-a no aplicativo ALTIVE DEV AI e **conseguiu entrar com sucesso**. Registrar como **teste manual aprovado pelo usuário**, ainda sem inspeção independente dos logs de servidor.

- A nova chave foi emitida pelo painel real e validada pelo aplicativo; **não registrar nem copiar o valor da chave, token de ativação ou credenciais**.
- Adicionar teste automatizado contra backend controlado/simulado e testes negativos (chave inválida, vencida, bloqueada, limite de dispositivos, rede offline, sessão renovada, logout), sem tocar em licenças de clientes.
- Manter credenciais criptografadas e separadas de chaves API; não introduzir regressão na ativação concluída.
