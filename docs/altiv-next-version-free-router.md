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


## Refinamento premium automático e preview local obrigatório — feedback Barbearia CRS, 15/11/2026

### Evidência do teste
O usuário enviou dados públicos de uma ficha no Google e quatro fotografias para criar a landing page da **Barbearia CRS**. O agente afirmou ter criado React/Vite/TS, 7 cards, galeria e contato e apresentou o projeto em `http://localhost:5173` depois de o usuário solicitar o preview manualmente. As capturas mobile e de página completa mostram estrutura e imagens funcionais, mas também iconografia simples/colorida, baixo contraste em blocos claros (especialmente avaliação Google), letras pequenas, hero e menu pouco sofisticados. Não afirmar aprovação de interação/desktop sem testes independentes; a imagem fornecida é um screenshot comprimido.

### Regras comportamentais propostas para SOUL.md + GLOBAL.md (não configuradas automaticamente no aparelho)
- **Pesquisa de referências antes de construir:** quando o pedido for criar ou modernizar um site e a ferramenta de navegação/web estiver conectada, pesquisar 2 a 4 referências verificáveis e recentes do segmento (por exemplo barbearia, odontologia, delivery), detectar padrões de layout, paleta, tipografia, CTA e experiência mobile. Não copiar identidade ou fotos de terceiros sem licença; fazer síntese original. Se não houver acesso à web, usar design system consolidado e informar limitação, sem inventar pesquisas.
- **Acabamento premium por padrão:** identidade visual coerente com o cliente, títulos com hierarquia, tipografia adulta, ícones lineares monocrômicos (20–24 px usualmente), contraste WCAG AA, botões com toque confortável, consistência de padding/bordas/card, imagens reais com enquadramento e autorização, CTA de WhatsApp/agendamento funcional, nenhuma seção invisível ou espaço vazio excessivo. Evitar emojis decorativos, cores claras ilegíveis e avaliações fictícias.
- **Self-review:** avaliar visualmente o DOM e screenshots (somente se houver ferramenta de visão real), pelo menos um breakpoint mobile 360/390 e desktop 1440, além de typecheck/build; limitar retrabalhos a duas passagens e declarar explicitamente verificações não executadas.
- **Preview automático em toda criação E atualização de sites**: encontrar porta livre, iniciar/reativar o servidor correto no Linux interno (por exemplo Vite), validar porta e HTTP response, abrir captura quando possível, manter processo ativo durante teste e devolver link local real clicável ao final, sem esperar que o usuário peça 'me dê localhost'. Nunca inventar `localhost:5173` nem prometer servidor em execução sem verificação. Em edição de repositório remoto sem preview local, informar o impedimento e passar o link do deploy de preview somente após verificação.
- **Respeitar dados reais**: integrar dados e fotografias autorizados fornecidos pelo cliente, identificar informações não verificadas, não alegar acesso ao perfil privado do Google Business, não inventar avaliações, preços, contatos ou horários.
- **Concluir com evidências:** o resultado final sempre inclui resumo de alterações, testes, URL localhost verificada ou erro específico, e screenshot real quando disponível. O cliente não deve conhecer comandos `npm run dev`.

### Próxima implementação no APK (memória não garante execução)
1. Adicionar um **post-task preview lifecycle** no agente/Visual Builder para tarefas de criação ou edição web; identificar `package.json`, comando de preview, porta livre e servidor reutilizável; obter URL pela saída do processo e efetuar health check de rota/página.
2. Persistir metadados de preview por workspace, não por chat somente; botão 'Abrir preview' no resultado final, evitar processos órfãos e limpar ao encerrar o projeto.
3. Orquestrador de skills de pesquisa de referência / direção de arte / Visual QA (aproveitar 12 skills web já instaladas, sem duplicá-las), seleção por tarefa, limitações de visão.
4. Aferir contraste e tamanho dos ícones; validar navegação e conteúdo em mobile/desktop. Guardar screenshots e diferenciar `testado` de `não testado`.
5. Regressão de localhost ao atualizar projeto existente: garantir que o servidor mostra a **nova** versão (hot reload ou reinício), não cache ou outra porta/projeto.

### Critérios de aceite
- 'Crie uma landing page da barbearia' -> recebe URL localhost comprovadamente funcionando, sem perguntar endereço/porta ao cliente, e captura quando possível.
- 'Corrija o visual da página' -> recebe URL da versão atualizada e resultado de testes sem novo prompt.
- Pesquisa realizada de referências quando há navegador disponível, com links consultados; sem navegador, nenhuma referência inventada.
- Tipografia e contraste legíveis em 360/390/1440 px; ícones discretos, fotos carregadas e controles funcionais.
- Sem ferramentas em loops infinitos, sem consumo surpresa de serviços pagos, sem sobrescrever dados de clientes.
