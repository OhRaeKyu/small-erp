-- 로그인 연속 실패 잠금 (5회 실패 시 10분)
alter table users
    add column failed_login_count int not null default 0,
    add column locked_until timestamptz;

alter table platform_admins
    add column failed_login_count int not null default 0,
    add column locked_until timestamptz;
