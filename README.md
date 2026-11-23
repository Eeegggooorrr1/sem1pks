# Сервис объявлений

Сервис объявлений с поиском, категориями, тегами, заметками и архивом. Есть массовые операции,
импорт и экспорт CSV, аналитика и управление пользователями. Backend написан на Spring Boot,
данные хранятся в PostgreSQL. 

## Запуск

1. Скопируйте пример настроек в корневой каталог:

```sh
cp .env.example .env
```
Для локального запуска можно скопировать все значения из `.env.example` как есть.

2. Запустите проект:

```sh
make start
```
Без make выполните `docker compose --env-file .env up --build -d --wait frontend`.


После запуска:

- сайт: http://localhost:5173
- API и Swagger доступны на http://localhost:8080/swagger-ui/index.html
- администратор: `admin@example.com / Admin-local-123`
- демопользователи: `demo1@example.com` ... `demo5@example.com`
- пароль демопользователей: `Demo-local-123`
- CSV скачивается через `/api/listings/export`, общий экспорт администратора - через `/api/admin/export`


## Настройки


| Параметр | Назначение                         |
| --- |------------------------------------|
| `BACKEND_PORT` | Порт API на компьютере             |
| `FRONTEND_PORT` | Порт сайта, по умолчанию в примере 5173 |
| `POSTGRES_PORT` | Порт PostgreSQL на компьютере      |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Данные PostgreSQL                  |
| `APP_JWT_ACCESS_SECRET` | Секрет JWT, >= 32 байт             |
| `APP_JWT_ACCESS_EXPIRATION_MILLISECONDS` | Время жизни токена                 |
| `BOOTSTRAP_ENABLED` | Включить создание стартовых учетных записей |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | Учетная запись администратора      |
| `BOOTSTRAP_DEMO_ENABLED` | Создать пять демопользователей     |
| `BOOTSTRAP_DEMO_PASSWORD` | Пароль демопользователей           |

Если порт занят, измените `FRONTEND_PORT`, `BACKEND_PORT` или `POSTGRES_PORT`. После изменения выполните
`make up`.
Внутренние адреса PostgreSQL и backend Compose задает сам, поэтому их не нужно добавлять в `.env`.
При первом запуске подойдет пустая база. Flyway создаст таблицы и заполнит справочник категорий. Bootstrap создаст администратора и, если включены демопользователи, пять обычных аккаунтов. Каталог объявлений изначально пуст.

## Команды

| Команда | Действие |
| --- | --- |
| `make start` / `make up` | Запустить сайт, backend и PostgreSQL |
| `make build-backend` | Собрать образ backend |
| `make build-frontend` | Собрать образ сайта |
| `make build` | Собрать backend и сайт |
| `make logs` | Смотреть логи backend |
| `make ps` | Посмотреть состояние контейнеров |
| `make stop` | Остановить контейнеры |
| `make down` | Удалить контейнеры, сохранив данные |
| `make test-backend` | Проверить бэкенд на PostgreSQL |
| `make test-frontend` | Тесты HTTP-клиента, проверка TypeScript и сборка сайта |
| `make test` | Проверить backend и frontend |
| `make test-e2e` | Проверить сайт в Chromium с настоящим API и отдельной тестовой БД |
| `make test-down` | Удалить тестовые контейнеры |




