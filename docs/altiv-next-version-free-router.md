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
