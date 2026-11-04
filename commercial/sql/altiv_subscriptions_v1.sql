-- ALTIV DEV AI | commercial service entitlements (new, dedicated Supabase project only)
-- GPLv3 app remains usable and distributable; this table authorizes ALTIV-hosted
-- paid services, not ownership of the APK or access to customers' local files.
--
-- Deployment is deliberately NOT performed by this PR.
create table if not exists public.altiv_service_subscriptions (
    user_id uuid primary key references auth.users(id) on delete cascade,
    plan_code text not null default 'monthly' check (plan_code in ('monthly', 'trial')),
    status text not null default 'pending'
        check (status in ('pending', 'active', 'suspended', 'cancelled')),
    valid_from timestamptz not null default now(),
    valid_until timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint altiv_valid_dates check (valid_until > valid_from)
);

alter table public.altiv_service_subscriptions enable row level security;
-- Self-service clients can read ONLY their own entitlement; no client writes.
revoke all on public.altiv_service_subscriptions from public, anon, authenticated;
grant select (user_id, plan_code, status, valid_from, valid_until)
    on public.altiv_service_subscriptions to authenticated;

drop policy if exists altiv_subscription_read_own on public.altiv_service_subscriptions;
create policy altiv_subscription_read_own
on public.altiv_service_subscriptions for select to authenticated
using ((select auth.uid()) = user_id);

-- The expiration comparison happens on the server's clock, not the Android clock.
-- SECURITY INVOKER respects row-level security, even if called through PostgREST.
create or replace function public.altiv_my_subscription()
returns table (
    is_active boolean,
    plan_code text,
    subscription_status text,
    expires_at timestamptz,
    checked_at timestamptz
)
language sql
stable
security invoker
set search_path = ''
as $$
    select
        (s.status = 'active' and s.valid_from <= now() and s.valid_until > now()) as is_active,
        s.plan_code,
        s.status as subscription_status,
        s.valid_until as expires_at,
        now() as checked_at
    from public.altiv_service_subscriptions as s
    where s.user_id = (select auth.uid())
    limit 1;
$$;
revoke all on function public.altiv_my_subscription() from public, anon;
grant execute on function public.altiv_my_subscription() to authenticated;

-- Never grant INSERT / UPDATE / DELETE to anon or authenticated.
-- Provision and renew via an authorized admin process on the backend only.
