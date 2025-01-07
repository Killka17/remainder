# Инструкция по запуску проекта

Для того чтобы запустить проект, выполните следующие шаги:

1. Напишите команду `/start` боту [@notifications_scala_bot](https://t.me/notifications_scala_bot).
2. Вставьте полученный от бота `CHAT_ID` в файл `docker-compose.yml`.
3. Выполните команду для сборки и запуска проекта:
    ```bash
    docker-compose up --build
    ```

---

### Маппинг таблицы

Для работы с базой данных требуется создать таблицу в схеме `remainder`. Используйте следующий SQL-запрос для создания таблицы:

```sql
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
