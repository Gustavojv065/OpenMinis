# ALTIV DEV AI — MVP de acesso comercial e assinaturas de 30 dias

## Situação atual
Esta branch adiciona uma **entrada de conta** no APK ALTIV, consulta da assinatura via Supabase Auth + RLS e uma landing page de apresentação (`commercial/site/index.html`). **Não implanta o banco, não ativa pagamentos e não gera credenciais reais.**

O código Android deriva do OpenMinis sob GPL-3.0. A assinatura autoriza **serviços hospedados e suporte comerciais da ALTIV**; ela não impede que o usuário exerça seus direitos legais sobre o software GPL, e nunca remove arquivos locais do cliente.

## Configuração segura — somente em projeto Supabase comercial dedicado
1. Criar um projeto separado de ambientes de clientes e sistemas ALTIV já existentes. Não reutilizar Supabase do PDV, Beauty Agenda, Vitrine ou Gestão.
2. Em Supabase Auth, ativar e-mail/senha com confirmação de e-mail; revisar domínio, SMTP, limites e política de privacidade.
3. Revisar e aplicar a migração `commercial/sql/altiv_subscriptions_v1.sql` no projeto **staging** dedicado. Auditar RLS antes de produção.
4. No ambiente de compilação do flavor ALTIV, fornecer **somente**:
   - `ALTIV_COMMERCIAL_URL=https://<project-ref>.supabase.co` (URL pública)
   - `ALTIV_COMMERCIAL_PUBLISHABLE_KEY=sb_publishable_...` (chave publicável, não secreta)
5. **Nunca** incluir `service_role`, secret key, senhas administrativas ou tokens no APK, no GitHub ou em prints.
6. Compilar `:app:assembleAltivDebug` e validar fluxo real em Android ARM64.
7. Para distribuição comercial, configurar uma assinatura de **release estável** no gerenciador de segredos, com plano de migração do APK debug.

Se as duas variáveis não estiverem presentes, a tela explica que o serviço comercial está em preparação e permite continuar com recursos locais, sem simular login.

## Fluxo de contratação inicial (manual)
1. O visitante acessa a landing e solicita contratação pelo WhatsApp; o valor não está fixado no código.
2. Após confirmar o pagamento **fora do APK**, um administrador cria o usuário por canal seguro no Supabase Auth e envia instruções de criação/recuperação de senha.
3. Administrador cria a assinatura para o **UUID exato** do usuário, com início e término determinados no servidor.

Exemplo de SQL administrativo (substituir por UUID real em ambiente dedicado; não executar de dentro do APK):

```sql
insert into public.altiv_service_subscriptions (user_id, plan_code, status, valid_from, valid_until)
values ('00000000-0000-0000-0000-000000000000'::uuid, 'monthly', 'active', now(), now() + interval '30 days')
on conflict (user_id) do update set
  status = 'active',
  valid_from = now(),
  valid_until = greatest(now(), public.altiv_service_subscriptions.valid_until) + interval '30 days',
  updated_at = now();
```

**Atenção:** SQL acima é ilustrativo. A ativação real pressupõe usuário existente e validação administrativa do pagamento. Para não conceder dias indevidos, revisar `valid_from` e data de renovação antes de executar.

## Regras e limites
- Login usa API pública de senha do Supabase, tokens ficam criptografados no Android Keystore.
- `altiv_my_subscription()` retorna somente a própria assinatura; a data é comparada com `now()` do Postgres, não com relógio do cliente.
- O app não aceita status premium enviado por `user_metadata`, parâmetro ou variável local como autoridade.
- Cada **endpoint ALTIV hospedado e pago** precisa verificar o mesmo direito de acesso no servidor (não confiar na tela). Os endpoints ainda não foram criados nesta fase.
- Ao vencer ou perder conexão, o acesso remoto pago não deve ser liberado sem validação. Conversas, arquivos, skills e trabalho com provedores que o usuário contrata separadamente continuam locais.
- A tela de conta no Android é o primeiro passo; ainda faltam painel Master, recuperação de senha com fluxos próprios, conciliação de pagamentos, política de privacidade comercial e canal de suporte formal.

## Homologação / critérios de aceite
- Sem configurar Supabase: a aplicação ALTIV continua abrindo em modo local; Minis original permanece inalterado.
- Conta válida com assinatura ativa: `altiv_my_subscription` responde `is_active=true`.
- Conta pendente, suspensa, cancelada ou expirada: `is_active=false`, mesmo que a data do aparelho seja alterada.
- Usuário A não lê/edita assinatura de B; usuário autenticado não possui privilégios INSERT/UPDATE/DELETE.
- Credenciais erradas, desconexão e falha do backend exibem erros claros sem log de tokens.
- Logout e troca de conta não apagam arquivos e chats.
- Executar tests Kotlin + build ARM64 no GitHub Actions; depois testes reais em dispositivo.

## Próximas fases
1. Painel Master web com RBAC independente, convite e renovação com auditoria.
2. Checkout e webhook de pagamento autenticado (Stripe, Mercado Pago ou outro provedor escolhido); idempotência, retries e chargebacks.
3. Backend premium com verificação por requisição, rate limits, métricas de uso, suporte, notificações de vencimento e termos.
4. Build `altivRelease` assinado com chave **estável**, canal de atualização e testes E2E.
