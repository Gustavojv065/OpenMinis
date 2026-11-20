package com.openminis.app.data.repository

/**
 * ALTIVE-authored skills bundled with the Android commercial flavor.
 *
 * These are ORIGINAL instruction sets for the existing skill engine, not copies
 * of third-party ChatGPT plugins or authenticated connections to their services.
 * They never call an external API on their own.
 */
internal object AltivBundledSkills {
    val contents: List<String> = listOf(
        """
        ---
        name: altive-prompt-studio
        version: 1.0.0
        description: Gerador inteligente de prompts para landing pages, lojas, barbearias, sites e SaaS a partir de briefing e fontes públicas autorizadas.
        ---

        # ALTIVE Prompt Studio

        Use quando o cliente pedir "gere um prompt", "crie um prompt", "use Google/Instagram" ou quiser transformar um briefing em projeto.

        1. Entenda ramo, público, objetivo, estilo, páginas, ações do cliente e restrições. Use informações fornecidas e links públicos que as ferramentas realmente conseguirem consultar; não invente dados ausentes.
        2. Separe FATO VERIFICADO, DADO FORNECIDO e CAMPO PENDENTE. Nunca afirme ter lido um perfil privado ou executado pesquisa sem evidência.
        3. Gere um prompt único, pronto para copiar: objetivo, identidade visual, seções, imagens com licença compatível, funcionalidades, responsividade, acessibilidade, SEO básico, orçamento e critérios de aceite.
        4. Para negócios com Google/Instagram, solicite URLs e arquivos apenas se necessários; observe direitos das imagens e políticas da fonte. Não utilize credenciais de redes sociais copiadas do navegador.
        5. Se a pessoa pediu só o prompt, ENTREGUE APENAS O PROMPT, sem executar comandos. Se pediu projeto, execute o projeto com as ferramentas disponíveis, preservando outros trabalhos.
        6. Inclua checklists verificáveis de build, localhost, mobile, desktop e formulários; não declare testes não executados.
        7. Prefira respostas objetivas em pt-BR e evite a repetição do briefing inteiro a cada rodada.
        """.trimIndent(),
        """
        ---
        name: altive-ux-frontend-premium
        version: 1.0.0
        description: Direção de arte UI UX e frontend premium para sites, landing pages, lojas e SaaS responsivos.
        ---

        # ALTIVE UX e Frontend Premium

        Use em criação ou refinamento de interfaces web.

        - Defina hierarquia tipográfica, grid, espaçamento, contraste e uma paleta coerente com o segmento REAL do negócio.
        - Priorize hero de impacto, texto legível, CTA claro, navegação acessível e componentes reutilizáveis.
        - Fotos e textos devem corresponder ao assunto: barbearia = barbeiro/cabelo/barba; moda = roupas; cafeteria = café. Nunca aceite imagem fora do contexto só porque carrega.
        - Prefira fotos com fonte, licença verificável e direitos de uso comercial; sem cópias de Instagram sem consentimento.
        - Valide 360px, 390px, 768px e 1440px. Verifique overflow horizontal, botões cortados, fontes muito pequenas e elementos flutuantes sobrepostos.
        - Preserve código funcional durante refinamentos e use instruções mínimas para evitar retrabalho e custos excessivos.
        - Caso faltem dados reais de WhatsApp, endereço, preços ou avaliações, marque explicitamente como demonstração; não apresente dado fictício como comercial.
        """.trimIndent(),
        """
        ---
        name: altive-3d-motion-design
        version: 1.0.0
        description: Microanimações e efeitos 3D sutis com acessibilidade e desempenho em Android e desktop.
        ---

        # ALTIVE 3D e Motion Design

        Use ao criar interfaces premium com profundidade ou animação.

        - Prefira CSS transform, perspective, sombras e transições leves a bibliotecas pesadas quando o efeito for apenas decorativo.
        - Respeite prefers-reduced-motion e teclado/foco. Movimentos nunca podem ocultar texto, bloquear botões ou prejudicar o formulário.
        - Faça lazy loading de assets decorativos e reduza efeitos em aparelhos de menor capacidade. Evite jank em Android de 4 GB.
        - Planeje fallback estático quando 3D/Canvas/WebGL não estiver disponível.
        - Execute build e verifique comportamento visual real; não trate a presença de uma classe CSS como prova de animação funcionando.
        """.trimIndent(),
        """
        ---
        name: altive-visual-quality-audit
        version: 1.0.0
        description: Auditoria visual e funcional real de sites: imagens, mobile, botões, checkout, agendamento, localhost e evidências.
        ---

        # ALTIVE Auditoria Visual e Funcional

        Use antes de afirmar que um site ou SaaS está pronto.

        1. Execute o build e registre código de saída. Verifique que o servidor responde no localhost.
        2. Teste telas 390px e 1440px; para páginas longas, role/capture em partes. Uma captura apenas do hero NÃO comprova a página inteira.
        3. Revise cada imagem por coerência semântica e qualidade; rejeite prédios em pet shop, navios em cafeteria e fotos repetidas ou quebradas.
        4. Teste menu, filtros, formulário, WhatsApp, quantidade do carrinho, total e agendamento. Diferencie validação local de persistência/servidor real.
        5. Verifique telefone: links de WhatsApp de demonstração não são válidos para homologação comercial. Não enviar pedido para contato de terceiros.
        6. Reporte falhas com arquivo/componente afetado e evidência. Execute uma rodada de correções focadas; evite loops ilimitados.
        7. Não declare como executado um teste que não conseguiu realizar. Não use códigos HTTP 200 como prova de qualidade do layout.
        """.trimIndent(),
        """
        ---
        name: altive-commerce-booking
        version: 1.0.0
        description: Fluxos comerciais para ecommerce e agendamento de barbearia, salão e serviços com WhatsApp, carrinho e reservas verificáveis.
        ---

        # ALTIVE Comércio e Agendamento

        Use para lojas, checkout WhatsApp e calendários de serviços.

        - Carrinho: mantenha itens, variações, tamanhos, cores, preço unitário, quantidade e total consistentes, inclusive depois de atualizar quantidades.
        - WhatsApp: número deve ser informado/autorizado pelo dono, usando formato internacional; encode a mensagem com dados reais. Nunca entregue número de exemplo como produção.
        - Agendamento: serviço, profissional, data, horário, nome e contato. Horários disponíveis devem vir de fonte real ou ser identificados como demonstração.
        - Sem backend e proteção contra corrida, não declare horário confirmado nem prometa ausência de reservas duplicadas; ofereça solicitação de horário com confirmação manual.
        - Formulários devem validar campos e acessibilidade; envio de sucesso simulado precisa ser explicitamente identificado.
        - Testes devem cobrir fluxo de compra com dois produtos e mudança de quantidades, WhatsApp preenchido, disponibilidade de horário, horário indisponível e layout mobile.
        """.trimIndent(),
        """
        ---
        name: altive-design-system-handoff
        version: 1.0.0
        description: Transformar referências Figma e design systems autorizados em componentes, tokens CSS e interfaces responsivas.
        ---

        # ALTIVE Design System e Figma
        
        - Use links Figma apenas quando o MCP ou material enviado realmente permitir acesso; sem fingir uma conexão inexistente.
        - Extraia paleta, tipografia, tokens, componentes e estados de interface com rastreabilidade à fonte.
        - Crie componentes React reutilizáveis e preview; não copie layouts protegidos de terceiros sem autorização.
        - Compare o resultado com a referência em mobile e desktop; confirme limitações de acesso antes de executar.
        """.trimIndent(),
        """
        ---
        name: altive-github-safe-workflow
        version: 1.0.0
        description: Fluxo seguro GitHub para modificar repositórios com branch, commit, pull request, testes e autorização.
        ---

        # ALTIVE GitHub Seguro
        
        - Descubra o repositório e branch corretos antes de editar. Preserve código, segredos e configurações existentes.
        - Faça alterações pequenas, com diffs claros, build e testes correspondentes.
        - Prepare pull request e informe link e arquivos modificados; merge e deploy exigem autorização expressa.
        - Nunca imprima tokens nem envie código privado a provedores sem permissão.
        """.trimIndent(),
        """
        ---
        name: altive-supabase-saas-security
        version: 1.0.0
        description: Projetar SaaS Supabase com autenticação, RLS multiempresa, migrações reversíveis e testes de isolamento.
        ---

        # ALTIVE Supabase SaaS Seguro
        
        - Modele tenants, papéis, permissões e trilha de auditoria antes das tabelas; nunca exponha service_role no cliente.
        - Cada tabela com dados de clientes deve ter RLS habilitada e políticas explícitas de isolamento entre empresas.
        - Faça migrações versionadas e backups seguros; testes com duas organizações devem provar que A não acessa B.
        - Só acesse projetos Supabase realmente conectados e autorizados; operações destrutivas pedem confirmação.
        """.trimIndent(),
        """
        ---
        name: altive-vercel-release-checklist
        version: 1.0.0
        description: Publicação segura no Vercel com preview, ambiente, domínio e rollback sem alterar produção por engano.
        ---

        # ALTIVE Publicação Vercel
        
        - Verifique build local e logs antes do deploy; distinguia preview de produção.
        - Confira variáveis de ambiente sem revelar segredos, domínio e proteção de rotas.
        - Prepare deploy em preview e mostre URL real; solicite autorização para promover à produção.
        - Se o Vercel não estiver conectado por MCP/API, entregue instruções e nunca invente deploy bem-sucedido.
        """.trimIndent(),
        """
        ---
        name: altive-project-planner
        version: 1.0.0
        description: Planejamento autônomo de projetos web com supervisores, tarefas pequenas e critérios de aceite.
        ---

        # ALTIVE Planejamento e Supervisores
        
        - Transforme objetivo do cliente em requisitos, módulos, critérios de aceite e riscos, preservando orçamento de tokens.
        - Divida trabalho em etapas pequenas; não repita diagnóstico completo a cada chamada.
        - Use supervisores como papéis de verificação e não faça cinco chamadas caras quando uma execução bastar.
        - Continue até concluir a tarefa possível com ferramentas disponíveis; registre bloqueios reais em vez de declarar 100% sem provas.
        """.trimIndent(),
        """
        ---
        name: altive-template-creator
        version: 1.0.0
        description: Criar templates originais de lojas, igrejas, clínicas, cafeterias e barbearias adaptados ao cliente.
        ---

        # ALTIVE Template Creator
        
        - Crie componentes e variações originais, reutilizáveis, com design responsivo e dados de demonstração rotulados.
        - Separe conteúdo, tema, páginas e configuração; valide mobile 360/390px e desktop.
        - Não reutilize imagens, dados pessoais ou segredos de um cliente para outro.
        - Inclua SEO básico, acessibilidade, estados vazios e botão de CTA testável; cite assets e licenças.
        """.trimIndent(),
        """
        ---
        name: altive-licensed-media-research
        version: 1.0.0
        description: Pesquisa de imagens reais pertinentes ao negócio, fontes licenciadas, atribuição e revisão semântica.
        ---

        # ALTIVE Imagens Reais e Licenças
        
        - Antes de buscar, identifique o tema e a intenção de cada seção: barbearia exige corte/barba; cafeteria exige café/produtos.
        - Pesquise pelo altive-media-search.py em Wikimedia/Openverse sem chave e Pexels/Pixabay/Unsplash apenas com credencial própria autorizada. Não presuma APIs conectadas ou acesso a fotos privadas.
        - Cheque licença, origem, necessidade de atribuição, domínio e conteúdo visual. Use arquivos locais só de fontes que permitam; fotos Unsplash API precisam de hotlink e atribuição. Prefira imagens reais adequadas em vez de SVG decorativo que não representa o serviço.
        - Faça download permitido, guarde créditos e valide carregamento, recorte, responsividade e ausência de duplicatas.
        """.trimIndent(),
        """
        ---
        name: altive-notion-project-docs
        version: 1.0.0
        description: Documentação de projetos e especificações técnicas para Notion quando a conta estiver conectada.
        ---

        # ALTIVE Documentação e Notion
        
        - Gere requisitos, decisões, tarefas, changelog e instruções de teste em Markdown rastreável.
        - Se Notion estiver conectado, confirme workspace e página autorizada antes de ler ou escrever.
        - Nunca declare publicação no Notion sem resposta bem-sucedida da ferramenta.
        - Respeite privacidade de clientes; não inclua tokens ou credenciais na documentação.
        """.trimIndent(),
    )
}
