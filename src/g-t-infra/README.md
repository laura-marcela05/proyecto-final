# g-t-infra — Implementation manual

Gym Track runs on one machine with `docker compose up`. This repository holds the
compose file and this manual; the code lives in `g-t-api`, `g-t-portal` and `g-t-db`.
The steps below follow `g-t-docs/10-devops/local-setup.md`, in the same order.

## 1. Prerequisites

| Tool | Version | Verify with |
|---|---|---|
| Docker Desktop, with Compose v2 | 4.x or newer | `docker compose version` |
| Git | 2.x | `git --version` |
| JDK 17 and Node 22 LTS — only to run the tests outside Docker | 17 / 22 | `java -version`, `node -v` |

Docker Desktop must be **running** before step 4.

## 2. Clone the four repositories side by side

```bash
mkdir gym-track && cd gym-track
git clone -b qa https://github.com/code-corhuila/g-t-infra.git
git clone -b qa https://github.com/code-corhuila/g-t-api.git
git clone -b qa https://github.com/code-corhuila/g-t-portal.git
git clone -b qa https://github.com/code-corhuila/g-t-db.git
```

`docker-compose.yml` expects exactly these folder names, beside `g-t-infra`.

## 3. Create `.env`

```bash
cd g-t-infra
cp .env.example .env        # Windows: copy .env.example .env
```

The example values start the application as they are. Each variable:

| Variable | Used by | Meaning |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `db`, `api` | Database name and credentials |
| `AUTH_JWT_SECRET` | `api` | HS256 signing key, at least 32 characters |
| `AUTH_JWT_EXPIRY_SECONDS` | `api` | Token life: `43200` = 12 hours |
| `CORS_ALLOWED_ORIGIN` | `api` | The only origin allowed to call the API |

## 4. Start

```bash
docker compose up --build
```

The first run downloads the images and builds the API (Maven) and the portal (npm),
so it takes several minutes. **Measured time: about 5 minutes** on the first run, from a clean clone.
The application is ready when the `api` log shows `Started GymTrackerApplication`.

## 5. Addresses

| What | URL |
|---|---|
| Portal — log in here | http://localhost:4200 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | `localhost:5432` |

## 6. Test users

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin123*` | `ADMIN` |
| `trainer1` | `Trainer123*` | `TRAINER` |
| `trainer2` | `Trainer123*` | `TRAINER` |

Demo values only, created by the seed of `g-t-db`.

## 7. Run the tests

| Tests | Command | Where |
|---|---|---|
| API | `./mvnw test` (Windows: `mvnw.cmd test`) | `g-t-api` |
| Portal | `npm ci`, then `npx ng test --watch=false --browsers=ChromeHeadless` | `g-t-portal` |
| End to end | Import `postman/gym-tracker.postman_collection.json` into Postman and **Run collection**, with the application up on a freshly seeded database (`docker compose down -v`, then `up`) | `g-t-api` |

## 8. Stop and erase the data

| Goal | Command, in `g-t-infra` |
|---|---|
| Stop, keep the data | `docker compose down` |
| Stop and erase the data — the next start runs the seed again | `docker compose down -v` |

## 9. Frequent problems

| Symptom | Cause | Fix |
|---|---|---|
| `error during connect` or `Cannot connect to the Docker daemon` when running `docker compose up` | Docker Desktop is not running | Open Docker Desktop, wait until it shows *Engine running*, and run the command again |
| `port is already allocated` or `address already in use` on 5432, 8080 or 4200 | Another program (a local PostgreSQL, another container) uses that port | Stop that program, or run `docker compose down` and start again |
| The portal opens but login fails; the browser console shows a CORS error | `CORS_ALLOWED_ORIGIN` in `.env` differs from the address in the browser's bar | Set `CORS_ALLOWED_ORIGIN=http://localhost:4200`, then `docker compose up --build` |
| A change to the seed or the schema has no effect | The `gt-db-data` volume already exists, and the init scripts run only on an empty volume | `docker compose down -v`, then `docker compose up --build` |
| `variable is not set` when starting | `.env` is missing or incomplete | Repeat step 3 |
| The `api` container stops with a database connection error | `db` is not ready yet, or the credentials in `.env` changed after the volume was created | Wait for `db` to be healthy; if you changed credentials, `docker compose down -v` |

---

Part of the **Gym Track** full-stack project — team `gym-track`, Complementaria III - Desarrollo Fullstack 2026-B.
Governance and documentation live in [`g-t-docs`](https://github.com/code-corhuila/g-t-docs).

## Branching

Three permanent branches. **None of them accepts a direct commit** — you enter through a child
branch and leave through a Pull Request.

```
develop  <--PR--  feat/... fix/... chore/...
qa       <--PR--  develop
main     <--PR--  qa (release)   hotfix/...
```

`develop` is the default branch: start every task from it. `main` requires **1 approval from
`ariel5253`**. On `develop` and `qa` the team sets its own review rule.

Commit and branch naming: `00-governance/git-conventions.md` in `g-t-docs`.
