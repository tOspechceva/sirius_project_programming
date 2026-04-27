# LAB7 - DB on separate node

## What was changed in app config

- `application.properties` uses external DB placeholders:
  - `DBHOST`
  - `DBPORT`
  - `DBNAME`
  - `SCHEMANAME`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- JDBC URL format:
  - `jdbc:postgresql://${DBHOST}:${DBPORT}/${DBNAME}?currentSchema=${SCHEMANAME}`

## Compose override for LAB7

Use `docker-compose.lab7.yml` together with base compose:

- app points to external host (`hl12.zil` by default)
- local postgres moved to optional profile `local-db`

## Start app with external DB

```bash
docker compose -f docker-compose.yml -f docker-compose.lab7.yml up -d --build app
```

## Example with explicit DB params

```bash
DBHOST=hl12.zil \
DBPORT=5432 \
DBNAME=hl6 \
SCHEMANAME=hl6 \
DB_USERNAME=hl6 \
DB_PASSWORD=pass_6 \
docker compose -f docker-compose.yml -f docker-compose.lab7.yml up -d --build app
```

## Check app startup

```bash
docker compose -f docker-compose.yml -f docker-compose.lab7.yml logs app --tail=120
```

## Bring up DB+pgAdmin on DB node (hl12)

Create a separate compose on `hl12` (outside this repository) with:

- postgres with command:
  - `postgres -c max_connections=1000`
- pgAdmin container

Then verify on DB node:

```bash
docker exec -it lab7-postgres psql -U <user> -d <db> -c "show max_connections;"
```
