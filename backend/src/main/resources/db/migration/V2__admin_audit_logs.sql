-- 관리자 작업 이력 (A-AUDIT-01): 회원가입 승인·반려, 이후 사업장 정지 등
create table admin_audit_logs (
    id          bigint generated always as identity primary key,
    admin_id    bigint      not null references platform_admins (id),
    action      varchar(30) not null,
    target_type varchar(30) not null,
    target_id   bigint      not null,
    detail      jsonb       not null default '{}',
    created_at  timestamptz not null default now()
);

create index ix_admin_audit_logs_target on admin_audit_logs (target_type, target_id);
