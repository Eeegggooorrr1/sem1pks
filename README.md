# Сервис объявлений

Небольшой сервис объявлений с регистрацией, поиском, заказами и ролями пользователей.
Backend написан на Spring Boot, консольный клиент - на Java 17. Данные в PostgreSQL.

## Запуск

Нужен Docker и команда `make`.

1. Скопируйте пример настроек в корневой каталог:

```sh
cp .env.example .env
```
Настройки из `.env.example` подходят для локального запуска, можно ничего не колхозить.

2. Запустите проект:

```sh
make start
```
Если `make` не установлен, просто запускать код с соответствующих команд.


После запуска:

- API и Swagger доступны на http://localhost:8080/swagger-ui/index.html
- администратор: `admin@example.com / Admin-local-123`
- демопользователи: `demo1@example.com` ... `demo5@example.com`
- пароль демопользователей: `Demo-local-123`
- архивы экспорта сохраняются в папку `exports`


## Настройки


| Параметр | Назначение                         |
| --- |------------------------------------|
| `BACKEND_PORT` | Порт API на компьютере             |
| `POSTGRES_PORT` | Порт PostgreSQL на компьютере      |
| `REPOSITORY_PROFILE` | `jdbc` или `jpa`                   |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Данные PostgreSQL                  |
| `APP_JWT_ACCESS_SECRET` | Секрет JWT, >= 32 байт             |
| `APP_JWT_ACCESS_EXPIRATION_MILLISECONDS` | Время жизни токена                 |
| `BOOTSTRAP_ENABLED` | Включить создание стартовых данных |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | Учетная запись администратора      |
| `BOOTSTRAP_DEMO_ENABLED` | Включить демоданные                |
| `BOOTSTRAP_DEMO_PASSWORD` | Пароль демопользователей           |

Если порт занят, измените `BACKEND_PORT` или `POSTGRES_PORT`. После изменения выполните
`make up`. Чтобы запустить JPA вместо JDBC, укажите в `.env`:


Внутренние адреса PostgreSQL и backend Compose задает сам, поэтому их не нужно добавлять в `.env`.
Для запуска клиента отдельно задайте `API_URL` и `EXPORT_DIR` в окружении процесса.

## Команды

| Команда | Действие |
| --- | --- |
| `make start` | Запустить backend и открыть клиент |
| `make up` | Запустить PostgreSQL и backend |
| `make cli` | Открыть клиент |
| `make build` | Собрать образы |
| `make logs` | Смотреть логи backend |
| `make ps` | Посмотреть состояние контейнеров |
| `make stop` | Остановить контейнеры |
| `make down` | Удалить контейнеры, сохранив данные |
| `make test` | Запустить backend и frontend тесты |
| `make test-down` | Удалить тестовые контейнеры |
| `make smoke` | Запустить сквозную проверку API |


## Тесты

Тестовая конфигурация находится в `.env.test`, ее можно получить копированием
`.env.test.example`.

```sh
make test
```

