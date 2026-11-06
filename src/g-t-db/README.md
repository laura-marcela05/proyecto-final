# g-t-db — Schema, seed and E-R diagram

The database of Gym Track: PostgreSQL 16, four tables. The schema lives only here
(`g-t-docs/06-data/migrations.md`); the API validates it (`ddl-auto: validate`) and never changes it.

| File | What it is |
|---|---|
| `sql/01-schema.sql` | Tables `users`, `students`, `sessions`, `session_muscle_groups`, their constraints and indexes, as in `models.md` and `database-conventions.md` |
| `sql/02-seed.sql` | Demo data with fixed UUIDs: three users, three students and one closed session with two muscle groups |
| `docs/er-diagram.mmd` | Mermaid source of the E-R diagram |
| `docs/er-diagram.png` | The diagram, exported from the `.mmd` |

## How the scripts run

`g-t-infra/docker-compose.yml` mounts `sql/` as `/docker-entrypoint-initdb.d` of the `postgres:16-alpine` image.
PostgreSQL runs the files in name order, **once, while its volume is empty**: first the schema, then the seed.
To run them again, erase the volume: `docker compose down -v` in `g-t-infra`.

## Demo data

| Username | Role | Owns |
|---|---|---|
| `admin` | `ADMIN` | — |
| `trainer1` | `TRAINER` | Ana Torres (one closed session: chest and arms, 65 min) and Carlos Pérez (no sessions) |
| `trainer2` | `TRAINER` | Sofía Ramírez (no sessions) |

Passwords are stored only as BCrypt hashes (cost 10); the plain demo values are in the manual of `g-t-infra`.

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
