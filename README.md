# Сервис объявлений

Небольшой сервис объявлений с регистрацией, поиском, заказами и ролями пользователей.
Backend написан на Spring Boot, доступны консольный и JavaFX-клиенты на Java 17. Данные в PostgreSQL.


## Запуск

Нужны Docker и команда `make`. Для JavaFX-клиента дополнительно требуется JDK 17 или новее,
путь к нему задаётся через `JAVA_HOME` в `.env`.

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
| `JAVA_HOME` | Путь к JDK для локального запуска JavaFX |

Если порт занят, измените `BACKEND_PORT` или `POSTGRES_PORT`. После изменения выполните
`make up`. Основной запуск использует JPA; в `.env` должно быть:

```dotenv
REPOSITORY_PROFILE=jpa
```

Внутренние адреса PostgreSQL и backend Compose задает сам, поэтому их не нужно добавлять в `.env`.
Для запуска клиента отдельно задайте `API_URL` и `EXPORT_DIR` в окружении процесса.

## Команды

| Команда | Действие |
| --- | --- |
| `make start` / `make javafx` | Запустить backend и открыть JavaFX-клиент |
| `make console` / `make cli` | Запустить backend и открыть консольный клиент |
| `make up` | Запустить PostgreSQL и backend |
| `make build` | Собрать backend и JavaFX-клиент |
| `make build-console` | Собрать образ консольного клиента |
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

