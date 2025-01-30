# Простая социальная сеть

Реализованы регистрация/вход/авторизация с помощью jwt, CRUD на юзерах и постах, роли
(обычный юзер - может постить, цензор - удалять любые посты, админ - выдавать роли и удалять пользователей)

Для запуска:

```bash
  sbt docker:stage
```
```bash
  docker-compose up
```

[localhost:1234/docs](http://localhost:1234/docs/) - SwaggerUI

###### Как стать админом:

+ `docker exec -it postgres-db psql -U postgres -d network`
+ `INSERT INTO user_roles (user_id, role_id)
  VALUES (`*user_id*`, `*role_id*`);`
    + должно быть: user_id = 1 для первого юзера; role_id = 2 для цензора; role_id = 3 для админа

<!--

Перед запуском, чтобы отпустить кэширование докера:

docker-compose down --volumes
docker-compose build --no-cache

-->
