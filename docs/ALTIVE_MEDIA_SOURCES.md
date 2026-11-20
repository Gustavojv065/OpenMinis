# ALTIVE — Pesquisa inteligente de fotografias (Android ARM64)

## Funcionamento automático

A cada pedido para criar ou atualizar um site, as instruções do agente orientam:
1. identificar o **negócio e a ação** (ex.: estética automotiva é polimento, não abastecimento);
2. criar um plano de imagens por seção (hero, serviços, galeria, produtos), com critérios de aceitação/rejeição;
3. pesquisar por **assunto específico** usando o novo `/usr/local/bin/altive-media-search.py`;
4. conservar direitos/licenças, autoria e origem em `ALTIVE-PHOTO-CREDITS.json`;
5. usar `read_image` por visão real nas imagens selecionadas, quando houver um modelo/grupo habilitado; caso contrário marcar `NOT_VERIFIED` e não alegar aprovação;
6. verificar o site renderizado e links/CTAs em mobile e desktop.

A existência da ferramenta não comprova que todas as fontes estão conectadas. E **nenhuma foto é semanticamente aprovada apenas porque o download funcionou**.

## Fontes, chaves e entrega

| Fonte | Credencial | Uso no projeto |
| --- | --- | --- |
| Wikimedia Commons | não exige | JPG/PNG/WebP local com licença CC/PD compatível |
| Openverse | não exige para pesquisa básica (sujeita a limite) | **Descoberta apenas** de resultados; não baixar de servidores arbitrários |
| Pexels | `ALTIVE_PEXELS_API_KEY` | download de imagem local de `images.pexels.com` e crédito |
| Pixabay | `ALTIVE_PIXABAY_API_KEY` | download de imagem local de `cdn.pixabay.com` e crédito |
| Unsplash | `ALTIVE_UNSPLASH_ACCESS_KEY` | hotlink oficial `photo.urls`, atribuição obrigatória e requisição ao `download_location` quando escolhido; **não copiar para o projeto** |

As três chaves são **opcionais**. O aplicativo jamais deve exibir, registrar em créditos ou expor no prompt as credenciais. Uma chave de OpenAI/ChatGPT/Gemini não é uma chave da API de fotos.

### Configurar uma fonte no próprio APK (sem Termux)

1. Obter a chave no site oficial do provedor de fotos, respeitando limites e uso comercial.
2. Abrir **ALTIVE → Configurações → Variáveis de ambiente → +**.
3. Em *Nome*, usar uma destas variáveis, com o valor da chave na caixa protegida: `ALTIVE_PEXELS_API_KEY`, `ALTIVE_PIXABAY_API_KEY`, `ALTIVE_UNSPLASH_ACCESS_KEY`.
4. Habilitar a variável. Os valores ficam criptografados no armazenamento Android existente, mas variáveis habilitadas são acessíveis pelos comandos/shells dos projetos e MCPs executados no mesmo ambiente. Nunca executar scripts não confiáveis com chaves sensíveis habilitadas.
5. Criar um novo site por linguagem natural; o agente deverá usar as fontes habilitadas. Se a fonte falhar, registrar motivo e continuar pelas fontes gratuitas.
6. Desabilitar ou excluir chaves não utilizadas.

Exemplo de comando interno para o agente (não é necessário o cliente executar):

```bash
python3 /usr/local/bin/altive-media-search.py \
  --source auto --niche automotive-detailing --role hero \
  --query "professional car detailing polishing" \
  --match-any polishing --match-any detailing \
  --exclude-any "gas station" --limit 3
```

O arquivo de saída inclui `images[*].src` (URL local `/assets/real-photos/...` **ou** URL oficial Unsplash), `delivery`, `source`, `visual_verdict=NOT_VERIFIED`, autoria e licença. O projeto só pode declarar QA visual depois de comparar pixels reais com a finalidade de cada foto.

## Segurança, custos e limites

- Redes permitidas por hostname HTTPS estrito; downloads de imagens limitados a 8 MiB, validados por MIME e assinatura JPG/PNG/WebP, sem redirecionar para hosts desconhecidos.
- Não baixa URLs arbitrárias retornadas pelo Openverse (reduz SSRF e arquivo malicioso); apenas lista os resultados para revisão.
- Mantém `ALTIVE-PHOTO-CREDITS.json` por projeto e não sobrescreve mídias antigas, usuários ou fotos enviadas.
- API Pexels normalmente tem uma cota grátis por chave; não presumir uso ilimitado. Pixabay/Unsplash também têm limites.
- Não inicia cobranças, integrações pagas, publicação ou troca de modelos sem consentimento. O buscador não instala outras dependências Python e não requer Termux.
- Direitos de imagem, retratos de pessoas e marcas comerciais ainda exigem conferência conforme o uso do cliente. Imagens de banco representam ilustrações e não a equipe/loja/produto exato.

## Fonte oficial (consultada em novembro de 2026)

- Pexels: https://www.pexels.com/api/documentation/ e https://www.pexels.com/license/
- Pixabay: https://pixabay.com/api/docs/
- Unsplash: https://help.unsplash.com/en/articles/2511245-unsplash-api-guidelines e https://unsplash.com/api-terms
- Openverse: https://api.openverse.org/v1/
- Wikimedia: https://commons.wikimedia.org/wiki/Commons:Licensing

## Aceite para a versão

- Rodar testes Python offline e Gradle no workflow ARM64.
- Conferir primeira geração de barbearia, pet shop e estética automotiva em Android físico; fotos precisam ter assunto correto, atribuição adequada, dimensões legíveis e não repetirem sem justificativa.
- Sem chaves opcionais, a versão deve funcionar com Wikimedia e mostrar resultados Openverse como discovery-only.
- Com chave configurada, validar pelo menos uma imagem de Pexels e outra de Pixabay em conta de teste, sem revelar a chave e respeitando as cotas.
- Unsplash deve usar hotlink, atribuição e tracking; sem chave, não afirmar que houve integração.
