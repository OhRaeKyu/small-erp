-- 사업장 정보 화면(U-080)에서 고치는 주소·연락처
alter table tenants
    add column address varchar(200),
    add column phone   varchar(20);
