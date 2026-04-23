# LAB4 - Performance Testing with K6

В LAB4 добавлено нагрузочное тестирование с K6 для API приложения.

## Что добавлено и изменено относительно LAB3

- Развернут K6 в `docker-compose` как отдельный сервис (`profile: perf`).
- Добавлен K6 профиль и тестовый скрипт:
  - `k6/profile.js`
  - `k6/load-test.js`
- Реализована смешанная нагрузка `POST + GET` в пропорции 50/50 (настраиваемо).
- Использован executor `ramping-vus` и стандартный пакет `k6/http`.
- Добавлен сценарий теста удвоения нагрузки (4-5 точек):
  - `k6/run-doubling.ps1`
- Добавлен JS-генератор графика и артефактов:
  - `k6/generate-chart.js`
  - генерирует:
    - `k6/results/avg-response-vs-vus.csv`
    - `k6/results/avg-response-vs-vus.md`
    - `k6/results/avg-response-vs-vus.html`

## Целевые endpoint-ы для нагрузки

- `POST /api/users` - создание простой сущности (пользователь).
- `GET /api/progress/users` - дополнительный endpoint со статистикой прогресса.

## Профиль нагрузки K6

Файл: `k6/profile.js`

Параметры настраиваются через переменные окружения:

- `BASE_URL` (default: `http://localhost:8082`)
- `POST_RATIO` (default: `0.5`)
- `TARGET_VUS` (default: `10`)
- `START_VUS` (default: `1`)
- `RAMP_UP` (default: `20s`)
- `STEADY_DURATION` (default: `40s`)
- `RAMP_DOWN` (default: `15s`)
- `SLEEP_SECONDS` (default: `0.2`)

## Как развернуть стенд (app + db)

```bash
docker compose up -d --build
```

Порты:

- API: `http://localhost:8082`
- PostgreSQL: `5432`

## Как запустить K6 вручную

Пример запуска для 20 VUs:

```bash
docker compose run --rm k6 run /scripts/load-test.js \
  -e BASE_URL=http://app:8081 \
  -e TARGET_VUS=20 \
  -e POST_RATIO=0.5 \
  -e RAMP_UP=20s \
  -e STEADY_DURATION=40s \
  -e RAMP_DOWN=15s \
  --summary-export /scripts/results/summary-vus-20.json
```

## Тест удвоения нагрузки (4-5 точек)

Готовый скрипт для Windows PowerShell:

```powershell
.\k6\run-doubling.ps1
```

Скрипт выполняет тесты по VUs:

- 10
- 20
- 40
- 80
- 160

## Построение графика зависимости avg response от VUs

После тестов запустить:

```bash
node k6/generate-chart.js
```

Результаты:

- CSV: `k6/results/avg-response-vs-vus.csv`
- Markdown-таблица: `k6/results/avg-response-vs-vus.md`
- HTML-график: `k6/results/avg-response-vs-vus.html`

## Что показать преподавателю

1. Поднятый compose-стенд (`docker ps`):
   - `hl-module1-postgres`
   - `hl-module1-app`
2. Запуск k6 теста (или `run-doubling.ps1`).
3. Файлы результатов в `k6/results`.
4. График `avg response vs vus` из `avg-response-vs-vus.html`.

## Примечание

Для стабильности в контейнерном режиме API опубликовано на `8082`,
внутри docker-сети приложение доступно как `http://app:8081`.
