# Online Banking System — Backend

Spring Boot REST API for a simulated online banking system. No real payment gateway, UPI network,
SMS or email is involved.

The full build spec lives in [INSTRUCTIONS.md](INSTRUCTIONS.md); this file is just how to run it.

| | |
|---|---|
| Java | 21 |
| Framework | Spring Boot 3.3.5 |
| Database | PostgreSQL 15+ |
| Security | Spring Security 6 + JWT (jjwt 0.12.6) |
| Build | Maven |
| Base package | `com.bank.online_banking_system` |
| Runs on | http://localhost:8080 |

---

## 1. Create the database

Once, before the first run:

```bash
createdb -U postgres online_banking
```

On Windows, `createdb` lives in `C:\Program Files\PostgreSQL\18\bin\` and may not be on your PATH:

```bash
"C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres -h localhost online_banking
```

Hibernate creates the tables on first startup (`ddl-auto=update`).

---

## 2. Configuration

Nothing secret is hardcoded. Every sensitive value is read from an environment variable with a
development fallback:

| Variable | Default | What it does |
|---|---|---|
| `DB_USER` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `JWT_SECRET` | a dev placeholder | HS256 signing key — **must be at least 32 characters** |
| `ADMIN_EMAIL` | `admin@obs.com` | Seeded administrator's email |
| `ADMIN_PASSWORD` | `Admin@123` | Seeded administrator's password |

The app logs a warning on startup while the default admin password is still in use.

### Option A — a local properties file (recommended)

The tidiest option: no environment variables at all, and it survives restarting your terminal or
your machine.

Copy the template that sits next to `application.properties`:

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Fill in your real values, then run with that profile:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

**The quotes matter in PowerShell.** Unquoted, PowerShell splits the argument at the hyphen inside
`spring-boot` and Maven receives `.run.profiles=local` as a separate argument, failing with
`Unknown lifecycle phase ".run.profiles=local"`. Quoting passes it through as one token. Git Bash
and macOS/Linux shells do not need the quotes, but they do no harm there either.

`application-local.properties` is already in [.gitignore](.gitignore), so your credentials never
reach the repository. Profile-specific properties override the defaults in `application.properties`.

If you would rather not type the flag every time, set the profile through the environment instead
— Spring reads `SPRING_PROFILES_ACTIVE` on its own:

```bash
[Environment]::SetEnvironmentVariable('SPRING_PROFILES_ACTIVE', 'local', 'User')
```

Then a plain `mvn spring-boot:run` picks up the profile in any new terminal.

### Option B — set them for one terminal session

Values disappear when you close the terminal. Good for a quick run.

**PowerShell:**

```bash
$env:DB_PASSWORD='your-postgres-password'; $env:JWT_SECRET='a-long-random-string-of-at-least-32-chars'; mvn spring-boot:run
```

**Git Bash / macOS / Linux:**

```bash
DB_PASSWORD='your-postgres-password' JWT_SECRET='a-long-random-string-of-at-least-32-chars' mvn spring-boot:run
```

Note that PowerShell uses `$env:NAME = 'value'` — the bash-style `NAME=value command` prefix is a
syntax error there.

### Option C — set them permanently for your Windows user

Survives new terminals and reboots. Run once in PowerShell:

```bash
[Environment]::SetEnvironmentVariable('DB_PASSWORD', 'your-postgres-password', 'User')
```

Repeat for each variable. **Open a new terminal afterwards** — existing ones keep the old
environment. Verify with:

```bash
$env:DB_PASSWORD
```

The same thing through the GUI: press `Win`, search "Edit the system environment variables" →
**Environment Variables…** → under *User variables*, **New…**.

### Option D — your IDE's run configuration

**IntelliJ IDEA:** Run → Edit Configurations → select `OnlineBankingSystemApplication` →
*Environment variables* → paste as a single semicolon-separated line:

```
DB_PASSWORD=your-postgres-password;JWT_SECRET=a-long-random-string-of-at-least-32-chars
```

**VS Code:** add an `env` block to the Java launch configuration in `.vscode/launch.json`.

### Which to use

Use **Option A** for day-to-day work and Option D if you run from the IDE. Reach for B or C only
when something outside the project needs the variables too.

---

## 3. Run

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Startup is clean when you see these two lines:

```
The following 1 profile is active: "local"
Started OnlineBankingSystemApplication in 14.485 seconds
```

If the first line is missing, the profile did not take and the app is running on the defaults from
`application.properties`. On first run it also logs `Seeded administrator 'admin' <admin@obs.com>`.

Package a runnable jar instead:

```bash
mvn clean package && java -jar target/online-banking-system-0.0.1-SNAPSHOT.jar
```

---

## 4. Tests

```bash
mvn test
```

41 integration tests across six classes. They run against an in-memory **H2** database, so
PostgreSQL does not need to be running and your real data is never touched.

| Class | Covers |
|---|---|
| `AuthModuleTest` | Registration, login, JWT, duplicate and weak-credential failures |
| `UserModuleTest` | Dashboard, profile, password change, cross-user isolation |
| `DepositWithdrawTest` | Balance movement, overdraft rejection, concurrent withdrawals |
| `TransferModuleTest` | Both transfer routes, two-row ledger, rollback, mutual-transfer deadlock |
| `HistoryUpiNotificationTest` | Pagination, filters, UPI resolve, notifications |
| `AdminModuleTest` | Stats, search, deactivation, role enforcement |

---

## 5. API

Swagger UI (backend running): **http://localhost:8080/swagger-ui.html**

Import [postman/OnlineBankingSystem.postman_collection.json](postman/OnlineBankingSystem.postman_collection.json)
and run the folders top to bottom — `{{token}}` is captured automatically after login.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| User | `GET /api/user/dashboard`, `GET|PUT /api/user/profile`, `PUT /api/user/change-password` |
| Money | `POST /api/transactions/{deposit,withdraw,transfer}` |
| History | `GET /api/transactions`, `GET /api/transactions/{reference}` |
| UPI | `GET /api/upi/me`, `GET /api/upi/resolve` |
| Notifications | `GET /api/notifications`, `PUT /api/notifications/{id}/read`, `DELETE /api/notifications/{id}` |
| Admin | `GET /api/admin/{stats,users,accounts,transactions}`, `PUT /api/admin/users/{id}/status` |

Every response uses the same envelope, so the frontend has one unwrap path:

```json
{ "success": true, "message": "Login successful", "data": { }, "timestamp": "…" }
```

Errors carry a stable `errorCode` (`INSUFFICIENT_BALANCE`, `DUPLICATE_RESOURCE`, `FORBIDDEN`, …)
and, for field validation, an `errors` map.

---

## 6. Troubleshooting

**`password authentication failed for user "postgres"`** — `DB_PASSWORD` is wrong or was never
picked up. If you set it permanently, open a new terminal.

**`column "status" of relation "users" contains null values`** — the database has tables from an
older schema version. Add the column nullable, backfill, then set `NOT NULL`; or drop and recreate
the database if the data is disposable.

**`app.jwt.secret must be at least 32 bytes`** — your `JWT_SECRET` is too short. Any 32+ character
string works.

**`Port 8080 was already in use`** — a previous run is still alive. Find and stop it:

```bash
netstat -ano | findstr :8080
```

**`Unknown lifecycle phase ".run.profiles=local"`** — a PowerShell quoting problem, not a Maven
one. Wrap the flag in double quotes: `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`. The
same applies to any `-D` flag whose value or name contains a hyphen, such as
`mvn test "-Dtest=AuthModuleTest"`.

**Login returns 403 instead of 401** — expected for a *deactivated* account (`ACCOUNT_DEACTIVATED`).
An admin can reactivate it from the Users screen.
