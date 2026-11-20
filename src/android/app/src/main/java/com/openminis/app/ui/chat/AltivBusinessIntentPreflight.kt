package com.openminis.app.ui.chat

/**
 * Durable first-pass semantic brief: attached automatically to web creation
 * and editing instructions, even in a new conversation with no saved memory.
 *
 * This is instruction policy, not a claim that images were checked by vision
 * or that any tool, provider, paid API or external integration was invoked.
 */
internal object AltivBusinessIntentPreflight {
    private data class Playbook(
        val niche: String,
        val triggers: List<String>,
        val requiredScenes: String,
        val rejectedScenes: String,
        val examples: String,
    )

    // More specific niches must precede generic ones (car wash != gas station).
    private val playbooks = listOf(
        Playbook(
            "automotive-detailing",
            listOf("estética automotiva", "estetica automotiva", "detalhamento automotivo",
                "car detailing", "auto detailing", "vitrificação", "vitrificacao",
                "polimento automotivo", "lavagem detalhada", "higienização automotiva",
                "higienizacao automotiva"),
            "HERO: profissional polindo ou lavando carro; SERVIÇOS: máquina de polimento " +
                "na pintura, aplicação de coating cerâmico, limpeza detalhada de bancos e interior; " +
                "GALERIA: antes/depois de detalhamento com autorização do dono.",
            "Posto de combustível, bomba de gasolina, abastecimento, prédio genérico, " +
                "fachada de posto ou carro aleatório sem ação de estética.",
            "--niche automotive-detailing --role hero --query \"professional car detailing polish\" " +
                "--match-any detailing --match-any polishing --match-any car " +
                "--exclude-any \"gas station\"",
        ),
        Playbook(
            "barbershop",
            listOf("barbearia", "barber shop", "barbershop", "barbeiro", "corte de barba"),
            "HERO: barbeiro cortando cabelo ou aparando barba; SERVIÇOS: tesoura, " +
                "máquina e toalhas em atendimento; GALERIA: cortes e ambiente de barbearia.",
            "Cabeleireiro de pets, escritório, paisagens, retratos genéricos sem serviço.",
            "--niche barbershop --role hero --query \"barber cutting hair\" --match-any barber",
        ),
        Playbook(
            "pet-shop",
            listOf("pet shop", "petshop", "banho e tosa", "veterinária", "veterinaria",
                "grooming pets"),
            "HERO: banho e cuidados com cães ou gatos; SERVIÇOS: tosa, banho, " +
                "consulta veterinária segundo serviço real; GALERIA: animais e profissionais.",
            "Prédios, postos, paisagens sem pets, produtos não relacionados e fotos de pessoas isoladas.",
            "--niche pet-shop --role hero --query \"dog grooming bath\" --match-any dog --match-any grooming",
        ),
        Playbook(
            "coffee-shop",
            listOf("cafeteria", "café artesanal", "cafe artesanal", "coffee shop",
                "espresso", "torrefação", "torrefacao"),
            "HERO: barista, xícara de café ou extração; CARDÁPIO: bebidas corretas " +
                "por categoria; GALERIA: café, grãos, latte art e ambiente real.",
            "Prédios de cidade, banco de praça, navios, ponte ou paisagens sem café.",
            "--niche coffee-shop --role hero --query \"barista espresso coffee\" --match-any coffee",
        ),
        Playbook(
            "fashion-store",
            listOf("loja de roupas", "moda feminina", "moda masculina", "fashion store",
                "boutique de moda", "vestidos", "coleção de roupas", "colecao de roupas"),
            "HERO: editorial de moda coerente; PRODUTOS: fotografias de roupas " +
                "correspondentes aos itens; GALERIA: tecidos, acessórios e looks.",
            "Animais, veículos, edifícios, objetos sem roupas e foto que não corresponde ao produto.",
            "--niche fashion-store --role hero --query \"fashion clothing model editorial\" --match-any fashion",
        ),
    )

    fun forRequest(request: String): String {
        val text = request.lowercase()
        val playbook = playbooks.firstOrNull { rule -> rule.triggers.any { text.contains(it) } }
        return buildString {
            appendLine("[ALTIVE FIRST PASS QUALITY PRECHECK — before creating files or sourcing photos]")
            appendLine("- FIRST understand the precise BUSINESS ACTION, not just a broad category.")
            appendLine("- Before code generation, create a compact slot-by-slot IMAGE BRIEF: hero, services,")
            appendLine("  cards, products and gallery, each with required subject, visual role and forbidden subject.")
            appendLine("- Use the correct existing ALTIVE skills and verified business facts; do not")
            appendLine("  borrow brands, client data or media from other projects.")
            appendLine("- Search with narrow terms PER SLOT and pass explicit --role, --match-any,")
            appendLine("  --niche when recognized and --exclude-any exclusions to altive-photo-search.py.")
            appendLine("- STOP and replace any candidate whose subject/scene does not match the exact slot.")
            appendLine("  A technically valid image is not semantic approval: use read_image when real")
            appendLine("  vision is available; otherwise report NOT_VERIFIED rather than guessing.")
            appendLine("- Do not build the final page around unverified random imagery. Keep a")
            appendLine("  visible asset checklist with missing slots; do not mark it all green.")
            appendLine("- Only after asset planning, implement the page and run build, localhost,")
            appendLine("  actual mobile/desktop screenshot review, and functional smoke tests.")
            appendLine("- One inexpensive PRECHECK before work avoids repeated expensive corrections.")
            if (playbook != null) {
                appendLine("- IDENTIFIED NICHE: ${playbook.niche}. Do not conflate it with adjacent sectors.")
                appendLine("- REQUIRED SCENES: ${playbook.requiredScenes}")
                appendLine("- REJECT SCENES: ${playbook.rejectedScenes}")
                appendLine("- EXAMPLE search arguments (adjust to the exact slot): ${playbook.examples}")
            } else {
                appendLine("- NICHE NOT PREMAPPED: derive required and forbidden scenes from the")
                appendLine("  user's exact business/service; do not use broad, unrelated stock photos.")
            }
            appendLine("- These rules apply in every NEW chat and on every web update; a rule")
            appendLine("  is not proof a tool or visual check actually ran.")
            append("[END ALTIVE FIRST PASS QUALITY PRECHECK]")
        }
    }
}
