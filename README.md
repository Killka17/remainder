маппинг таблички

create table remainder.remainders
(
    id             serial
        constraint remainders_pk
            primary key,
    remainder_time timestamp not null,
    remainder_text text      not null
);

alter table remainder.remainders
    owner to admin;

