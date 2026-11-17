-- Run in Supabase SQL Editor. The backend uses the Supabase secret key server-side.
create extension if not exists pgcrypto;

create table if not exists public.security_logs (
  id uuid primary key,
  severity text not null check (severity in ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
  source text not null,
  event_type text not null,
  message text not null,
  actor text,
  ip_address inet,
  created_at timestamptz not null,
  canonical_payload text not null,
  sha256 char(64) not null,
  transaction_id text,
  confirmed_round bigint,
  chain_status text not null default 'PENDING_CONFIGURATION',
  integrity_valid boolean
);
create index if not exists security_logs_created_at_idx on public.security_logs (created_at desc);
create index if not exists security_logs_severity_idx on public.security_logs (severity);

-- No browser access: only the trusted Java backend's secret key may access this table.
alter table public.security_logs enable row level security;
revoke all on table public.security_logs from anon, authenticated;
grant all on table public.security_logs to service_role;
