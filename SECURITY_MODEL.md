# Threat model: pranay-harness/todolist-demo

## System overview
- **Shipped app**: a Java servlet todo-list web app (`src/main/java`, `src/main/webapp`) built by Maven into `todolist.war` and run on `tomcat:8-jre8` (root `Dockerfile`, port 8080). The data store is an in-memory H2 DB (`db/ConnectionManager.java`: `jdbc:h2:mem:list;MODE=MYSQL`, user `sa`, empty password), so data is lost on restart.
- **Demo fixtures** (intentional, kept in the repo for scanner demos): `vulnerability/` (VulnerableApp-style Spring samples: SQLi, XSS, XXE, SSRF, JWT, IDOR, etc.), `idor-vuln.java`, `php-app/` (PHP image pinned to a vulnerable build, "Do not deploy"), and the sample secret in `Requirements.txt`.
- **CI/CD**: GitHub Actions in `.github/workflows` (Harness SAST/SCA via ShiftLeft, SLSA provenance, Scribe, CodeGuard with an OpenAI key, PHP CI, plus the deliberately unsafe `demo.yaml` and `pwn-request-threat.yml` `pull_request_target` workflows). Backstage templates are in `backstage.yaml` and `prod1.yaml`. Harness pipelines (STO/SSCA) run outside the repo.
- **Infrastructure**: no infrastructure repo (user confirmed). The root `Dockerfile` is the whole deployment definition.

## Components
| Component | Code |
|---|---|
| Auth | `src/main/java/login/`, `src/main/java/register/`, `src/main/java/inside/InsideFilter.java`, `login.jsp`, `index.jsp`, `loginFault.jsp`, `wrongRegister.jsp`, `userExists.jsp`, `loginFirst.jsp`, `NotUsed/` |
| Tasks | `src/main/java/inside/` (AddTask, Edit, Delete, Display), `src/main/webapp/inside/*.jsp`, `healthcheck/` |
| Data Store | `src/main/java/db/ConnectionManager.java`, schema in `register/Register.java` |
| Vulnerable Fixtures | `vulnerability/**`, `idor-vuln.java` |
| PHP Fixture | `php-app/**` |
| CI/CD | `.github/workflows/**`, `shiftleft.yml`, `backstage.yaml`, `prod1.yaml` |
| Build & Dependencies | `pom.xml`, `Dockerfile`, `package.json`, `Requirements.txt`, `my-webapp.iml`, `src/main/test1.ts` |

## Trust boundaries and entry points
1. **Internet → Tomcat** (unauthenticated): `/register`, `/login`, `/requestLogin`, `/health-check`, the top-level JSPs and static CSS.
2. **Session boundary**: `InsideFilter` guards `/inside/*` using the session attribute `name`, which `RequestLogin` sets. Behind it are `/inside/display`, `/inside/addTask`, `/inside/editTask`, `/inside/deleteTask` and the JSPs. All accounts are self-registered, so any internet user can get past this boundary.
3. **Per-user data boundary**: tasks are keyed by `name` + `createDate`. `Edit` scopes updates by session name, but `Delete` deletes by `createDate` only (cross-user delete is possible).
4. **GitHub → CI runners**: PRs from forks reach `pull_request_target` workflows, which have access to repository secrets (`SHIFTLEFT_ACCESS_TOKEN`, `SCRIBE_TOKEN`, `OPENAI_API_KEY`, `supersecret*`, inherited secrets) and to write tokens.
5. **Build → artifacts**: Maven/npm dependency resolution, Docker base images, SLSA provenance and the published image `pranaycshah/todolist-demo`.

## Assets (user-ranked)
- **Critical: user credentials and sessions.** The `accounts` table stores plaintext passwords (`Register.java`, `RequestLogin.java`). Session cookies get "remember me" lifetimes of up to 7 days.
- **Critical: CI secrets and the build/release pipeline.** This covers the Harness/ShiftLeft and Scribe tokens, the OpenAI key, `GITHUB_TOKEN` write scopes, SLSA provenance integrity and the published container image.
- Normal: users' task data (low sensitivity, held in memory only).

## Attacker profiles (user-confirmed)
- **Any internet user**, unauthenticated or holding a self-registered account. They attack other users' credentials, sessions and tasks, and the server itself.
- **External contributor / fork PR author** reaching the CI workflows. This counts as "any internet user" because GitHub accepts PRs from anyone. (Inferred from the user's answer; not separately confirmed.)

## Existing controls
- Most task SQL uses `PreparedStatement` (AddTask, Edit, Delete, Register insert). Login reads all accounts and compares in Java.
- `InsideFilter` enforces a session on `/inside/*`.
- SAST/SCA gates: ShiftLeft `build_rules` (no criticals, ≤1 OSS/container finding, no reachable OSS vulns), Harness STO/SSCA, CodeGuard.
- Some workflow actions are pinned by SHA (`github-slsa.yml`).

## Scope, accepted risk, priorities (user answers)
- The demo fixtures (`vulnerability/`, `idor-vuln.java`, `php-app/`, `demo.yaml`, `pwn-request-threat.yml`, the `Requirements.txt` sample secret) are **intentional and in scope**: report them at the scanner's severity, with no downgrade or dismissal.
- No attacker is ruled out within the internet-user profile.
- Findings that reach credentials/sessions or CI secrets/release integrity through a path the scanner confirmed may be raised. Task-data-only findings keep their severity.

## Open questions
- Is the WAR/image actually deployed anywhere reachable (e.g. a Harness demo environment), or only built?
- Are the `pull_request_target` workflows active on the GitHub repo, and do the referenced secrets really exist?
- Does the user want "Tasks" findings (e.g. cross-user delete) treated as more or less important than the default?
