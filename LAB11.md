# LAB11 — Kafka KRaft на Swarm (hl14 / hl15)

Чеклист под **вариант 6**: каталог `/home/hl/hl06`, топик **`hl06`** (имя внести в столбец **Kafka Topic** в [таблице](https://docs.google.com/spreadsheets/d/1CoubOXgx3PPpACLwhk_1lJ7QfoCFjmLf9jEzhSnu0qM/edit)).

Документация: [zil.digital — Kafka KRaft Swarm](https://zil.digital/blog/kafka-kraft-swarm-debian12), [kafka-2026.md (hl14/hl15)](https://bitbucket.org/zil-courses/hl-module2/src/main/kafka/kafka-2026.md?at=main).

| Узел   | Роль              | SSH (jump) |
|--------|-------------------|------------|
| hl14   | Worker, kafka-1   | `ssh -p 2314 hl@hlssh.zil.digital` |
| hl15   | Manager, kafka-2, Kafka UI | `ssh -p 2315 hl@hlssh.zil.digital` |

Пароль и пользователь — как в общей таблице курса (`hl` / пароль из методички).

---

## 1. Зайти на hl14 и hl15, посмотреть Swarm и Kafka

**На hl15 (manager):**

```bash
docker node ls
docker stack services kafka_stack
docker stack ps kafka_stack
docker service logs kafka_stack_kafka-2 -f
# по Ctrl+C выйти из логов
```

**На hl14 (worker):**

```bash
docker node ls
docker service logs kafka_stack_kafka-1 -f
```

(Если стек называется иначе — смотри `docker stack ls`.)

---

## 2. Папка по варианту

Выполнить на **hl15** или **hl14** (где удобнее; достаточно одного раза на том хосте, где работаешь):

```bash
mkdir -p /home/hl/hl06
chmod 755 /home/hl/hl06
```

---

## 3. Создать топик `hl06` и занести в таблицу

На **hl15**, если уже есть Kafka CLI как в `kafka-2026.md` (`~/kafka_2.13-3.7.1/`):

```bash
~/kafka_2.13-3.7.1/bin/kafka-topics.sh --create \
  --topic hl06 \
  --partitions 2 \
  --replication-factor 2 \
  --bootstrap-server localhost:9094
```

Проверка:

```bash
~/kafka_2.13-3.7.1/bin/kafka-topics.sh --describe \
  --topic hl06 \
  --bootstrap-server localhost:9094
```

Ожидается, что в **ISR** видны оба брокера (как для `test-topic` в методичке).

Дальше: в Google Sheets в своей строке в столбец **Kafka Topic** записать: **`hl06`**.

---

## 4. Kafka UI через SSH-туннель (с локальной машины)

Оставить сессию открытой. В **отдельном** терминале на ПК:

```bash
ssh -p 2315 -L 8080:127.0.0.1:8080 hl@hlssh.zil.digital
```

Браузер: **http://localhost:8080/** — должен открыться Kafka UI кластера на hl15.

---

## 5. Записать сообщения в топик `hl06` с локальной машины

Идея из **раздела 8** блога / **шаг 6** `kafka-2026.md`: проброс портов **9094** обоих брокеров и согласованные имена хостов для bootstrap.

### Вариант A — Linux / macOS (как в kafka-2026.md)

**Loopback-алиасы** (macOS — `ifconfig lo0`; Linux — `ip addr add`, см. блог).

В **`/etc/hosts`** на своём ПК:

```
127.0.0.2 hl15.zil
127.0.0.3 hl14.zil
```

**Один SSH** (порты как в kafka-2026 для hl14/hl15):

```bash
ssh -p 2315 \
  -L 8080:127.0.0.1:8080 \
  -L 127.0.0.2:9094:127.0.0.1:9094 \
  -L 127.0.0.3:9094:10.60.3.12:9094 \
  hl@hlssh.zil.digital
```

Локально (в каталоге распакованного Kafka CLI, версия совместима с кластером, напр. 3.7.1):

```bash
printf 'lab11-local-msg-1\nlab11-local-msg-2\n' | bin/kafka-console-producer.sh \
  --topic hl06 \
  --bootstrap-server hl15.zil:9094,hl14.zil:9094
```

Проверка чтения:

```bash
bin/kafka-console-consumer.sh \
  --topic hl06 \
  --bootstrap-server hl15.zil:9094,hl14.zil:9094 \
  --from-beginning \
  --group lab11-check-$(date +%s)
```

### Вариант B — Windows без loopback 127.0.0.2/3

На Windows проще пробросить **два разных локальных порта** на два брокера (в **PowerShell** или `cmd`, одна сессия SSH):

```text
ssh -p 2315 -L 8080:127.0.0.1:8080 -L 19094:127.0.0.1:9094 -L 19095:10.60.3.12:9094 hl@hlssh.zil.digital
```

Тогда producer/consumer:

```text
bin\kafka-console-producer.bat --topic hl06 --bootstrap-server localhost:19094,localhost:19095
```

(Путь к `kafka-console-producer` — в распакованном `kafka_2.13-3.7.1\bin` на Windows.)

---

## 6. Краткий чеклист сдачи

- [ ] Были сессии на **2314** и **2315**, выполнены команды Swarm / логи сервисов.
- [ ] Создана **`/home/hl/hl06`**.
- [ ] Создан топик **`hl06`**, есть вывод `--describe`.
- [ ] В таблице указан топик **`hl06`**.
- [ ] Открыт **Kafka UI** через `localhost:8080` с туннелем.
- [ ] С **локальной** машины отправлены **дополнительные** сообщения в **`hl06`** (и при желании прочитаны consumer’ом).
