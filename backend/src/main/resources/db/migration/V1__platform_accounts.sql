-- A. 플랫폼·계정 (1단계): 관리자, 업종 팩, 가입 신청, 사업장, 사용자, 소속

create table platform_admins (
    id            bigint generated always as identity primary key,
    email         varchar(255) not null unique,
    password_hash varchar(100) not null,
    name          varchar(50)  not null,
    role          varchar(20)  not null,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create table industry_packs (
    code                varchar(40)  primary key,
    name                varchar(100) not null,
    item_categories     jsonb        not null default '[]',
    order_option_schema jsonb        not null default '{}',
    default_faq         jsonb        not null default '[]',
    default_intents     jsonb        not null default '[]',
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now()
);

create table tenants (
    id                     bigint generated always as identity primary key,
    business_no            varchar(10)  not null unique,
    name                   varchar(100) not null,
    industry_code          varchar(40)  not null references industry_packs (code),
    status                 varchar(20)  not null,
    plan                   varchar(20)  not null default 'FREE',
    business_hours         jsonb,
    default_daily_capacity int,
    order_lead_days        int,
    settings               jsonb        not null default '{}',
    created_at             timestamptz  not null default now(),
    updated_at             timestamptz  not null default now()
);

create table signup_requests (
    id                   bigint generated always as identity primary key,
    business_no          varchar(10)  not null,
    company_name         varchar(100) not null,
    owner_name           varchar(50)  not null,
    industry_code        varchar(40)  not null references industry_packs (code),
    phone                varchar(20)  not null,
    email                varchar(255) not null,
    password_hash        varchar(100) not null,
    certificate_file_key varchar(255),
    status               varchar(20)  not null,
    reject_reason        varchar(500),
    reviewed_by          bigint references platform_admins (id),
    reviewed_at          timestamptz,
    tenant_id            bigint references tenants (id),
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now()
);

-- 같은 사업자번호로 대기 중인 신청은 하나만 (반려 이력은 남긴다)
create unique index ux_signup_requests_pending_business_no
    on signup_requests (business_no) where status = 'PENDING';
create index ix_signup_requests_status_created on signup_requests (status, created_at desc);

create table users (
    id            bigint generated always as identity primary key,
    login_id      varchar(255) not null unique,
    email         varchar(255) not null,
    password_hash varchar(100) not null,
    name          varchar(50)  not null,
    status        varchar(20)  not null,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create table memberships (
    id         bigint generated always as identity primary key,
    tenant_id  bigint      not null references tenants (id),
    user_id    bigint      not null references users (id),
    role       varchar(20) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (tenant_id, user_id)
);

insert into industry_packs (code, name, item_categories) values
    ('BAKERY_DESSERT', '베이커리·디저트', '["원재료","부자재","반제품","완제품"]'),
    ('HANDMADE_FOOD', '수제 식품 (청·잼·반찬 등)', '["원재료","부자재","반제품","완제품"]'),
    ('HANDMADE_CRAFT', '수제 공예품', '["원재료","부자재","완제품"]'),
    ('GENERAL', '기타 제조', '["원재료","부자재","반제품","완제품"]');
