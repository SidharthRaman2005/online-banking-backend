# Online Banking System — Backend Instructions

Authoritative build spec for the Spring Boot backend. Anything not written here is out of scope.
Frontend has its own spec at `../frontend/INSTRUCTIONS.md` — the API contracts in this file are the
single source of truth for both sides.

---

## 1. Stack & Ground Rules

| Concern | Choice |
|---|---|
| Language | Java 21 (JDK 21 is what this machine has installed) |
| Framework | Spring Boot 3.3.x |
| Database | PostgreSQL 15+ |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security 6 + JWT (jjwt 0.12.x) |
| Build | Maven |
| Docs | springdoc-openapi 2.x (Swagger UI) |
| Base package | `com.bank.online_banking_system` |

Non-negotiable rules:

1. **Money is always `BigDecimal`.** Never `double`, `float`, or `Double`. Column type
   `NUMERIC(19,2)`. Compare with `compareTo`, never `equals`.
2. **Controllers never touch repositories.** Controller → Service → Repository. Business logic and
   `@Transactional` live in the service layer only.
3. **Entities never cross the HTTP boundary.** Requests and responses are DTOs. A password hash must
   never appear in any response body, ever.
4. **Every state-changing banking operation writes a `Transaction` row** in the same database
   transaction that moves the money.
5. **Balance can never go negative.** Enforced in code *and* by a DB check constraint.
6. **No real integrations.** No payment gateway, no real UPI network, no SMS, no email. Everything is
   simulated inside this application.

---

## 2. Project Layout

```
backend/
└── src/main/java/com/bank/online_banking_system/
    ├── OnlineBankingSystemApplication.java
    ├── config/            SecurityConfig, OpenApiConfig, CorsConfig
    ├── security/          JwtService, JwtAuthenticationFilter, CustomUserDetailsService,
    │                      JwtAuthEntryPoint, JwtAccessDeniedHandler
    ├── entity/            User, BankAccount, Transaction, Notification
    ├── enums/             Role, AccountStatus, TransactionType, TransactionStatus
    ├── repository/        UserRepository, BankAccountRepository, TransactionRepository,
    │                      NotificationRepository
    ├── dto/
    │   ├── request/       RegisterRequest, LoginRequest, UpdateProfileRequest,
    │   │                  ChangePasswordRequest, AmountRequest, TransferRequest
    │   └── response/      ApiResponse, AuthResponse, UserProfileResponse, DashboardResponse,
    │                      AccountResponse, TransactionResponse, PageResponse,
    │                      AdminStatsResponse, ErrorResponse
    ├── service/           AuthService, UserService, AccountService, TransactionService,
    │                      TransferService, AdminService, NotificationService
    ├── controller/        AuthController, UserController, TransactionController,
    │                      AdminController, NotificationController
    ├── exception/         Custom exceptions + GlobalExceptionHandler
    └── util/              AccountNumberGenerator, UpiIdGenerator, ReferenceGenerator
└── src/main/resources/
    ├── application.properties
    └── application-dev.properties
```

---

## 3. Configuration

`application.properties`:

```properties
spring.application.name=online-banking-system
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/online_banking
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

app.jwt.secret=${JWT_SECRET:change-me-to-a-64-char-random-string-before-any-demo-run}
app.jwt.expiration-ms=86400000

app.account.initial-balance.min=100000
app.account.initial-balance.max=9900000
app.upi.domain=obs

springdoc.swagger-ui.path=/swagger-ui.html
```

Notes:
- `spring.jpa.open-in-view=false` is deliberate — it forces you to load what you need inside the
  service and prevents lazy-loading surprises in serialization.
- `ddl-auto=update` is fine for a college project. Never use it in production.
- The JWT secret must be at least 32 bytes for HS256. Keep it out of Git; read it from an env var.

Required Maven dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
`spring-boot-starter-security`, `spring-boot-starter-validation`, `postgresql`, `lombok`,
`jjwt-api` + `jjwt-impl` + `jjwt-jackson` (0.12.x), `springdoc-openapi-starter-webmvc-ui`.

---

## 4. Data Model

### 4.1 Enums

```java
public enum Role { USER, ADMIN }
public enum AccountStatus { ACTIVE, DEACTIVATED }
public enum TransactionType { DEPOSIT, WITHDRAW, TRANSFER }
public enum TransactionStatus { SUCCESS, FAILED }
```

### 4.2 `User`

| Field | Type | Constraints |
|---|---|---|
| `id` | Long | PK, identity |
| `username` | String | unique, not null, 3–20 chars |
| `email` | String | unique, not null, valid email, stored lowercase |
| `password` | String | not null, BCrypt hash |
| `role` | Role | not null, default `USER` |
| `status` | AccountStatus | not null, default `ACTIVE` |
| `createdAt` | Instant | `@CreationTimestamp` |
| `updatedAt` | Instant | `@UpdateTimestamp` |
| `bankAccount` | BankAccount | `@OneToOne(mappedBy="user", cascade=ALL)` |

Always normalise email to lowercase before saving *and* before lookup.

### 4.3 `BankAccount`

| Field | Type | Constraints |
|---|---|---|
| `id` | Long | PK |
| `accountNumber` | String | unique, not null, exactly 10 digits |
| `upiId` | String | unique, not null, `username@obs` |
| `balance` | BigDecimal | `NUMERIC(19,2)`, not null, `>= 0` |
| `status` | AccountStatus | not null, default `ACTIVE` |
| `createdAt` | Instant | `@CreationTimestamp` |
| `user` | User | `@OneToOne`, `@JoinColumn(name="user_id")`, unique |
| `version` | Long | `@Version` — optimistic locking |

Add the DB guard explicitly:

```java
@Table(name = "bank_accounts",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "account_number"),
           @UniqueConstraint(columnNames = "upi_id")
       })
@Check(constraints = "balance >= 0")
```

### 4.4 `Transaction`

| Field | Type | Notes |
|---|---|---|
| `id` | Long | PK |
| `transactionReference` | String | unique, e.g. `TXN` + 10 digits |
| `type` | TransactionType | DEPOSIT / WITHDRAW / TRANSFER |
| `amount` | BigDecimal | always positive; direction comes from `type` + accounts |
| `senderAccountNumber` | String | null for DEPOSIT |
| `receiverAccountNumber` | String | null for WITHDRAW |
| `status` | TransactionStatus | SUCCESS / FAILED |
| `description` | String | short human-readable line |
| `balanceAfterTransaction` | BigDecimal | see rule below |
| `createdAt` | Instant | `@CreationTimestamp` |
| `account` | BankAccount | `@ManyToOne` — the account this row belongs to |

**Ledger rule (read this twice).** A transfer touches two accounts, so it writes **two rows** that
share the same `transactionReference`:

- one row owned by the sender's account, `balanceAfterTransaction` = sender's new balance
- one row owned by the receiver's account, `balanceAfterTransaction` = receiver's new balance

`GET /api/transactions` filters on `account = currentUser.bankAccount`, so each user sees exactly
one row per transfer, with a balance figure that is correct *for them*. Deposits and withdrawals
write a single row. This avoids the classic bug where a shared `balanceAfterTransaction` column is
meaningless to one of the two parties.

Index `(account_id, created_at DESC)` — the history endpoint is the hottest query in the app.

### 4.5 `Notification` (Module 8, optional)

`id`, `user` (ManyToOne), `message`, `read` (boolean, default false), `createdAt`.

---

## 5. Generation Rules

**Account number** — 10 digits, first digit non-zero. Generate with `SecureRandom`, check
`existsByAccountNumber`, retry up to 5 times, then throw `TransactionFailedException`. Never derive
it from the user id (guessable).

**UPI ID** — `sanitize(username) + "@" + app.upi.domain`. Sanitize = lowercase, strip everything
outside `[a-z0-9._]`. If taken, append a 3-digit suffix (`sidharth1@obs`) and retry.

**Initial balance** — uniform random in `[min, max]` from config, rounded to 2 decimals,
`RoundingMode.HALF_UP`.

**Transaction reference** — `"TXN" + 10 random digits`, unique-checked.

**Critical invariants:**
- Account number, UPI ID and balance are generated **once, at registration only**.
- Login must not write to the database at all.
- Changing the username does **not** change the UPI ID. The UPI ID is a permanent payment address;
  silently re-pointing it would break every payer who saved it. State this in the profile UI.

---

## 6. Security

### 6.1 JWT

- Algorithm HS256, secret from config.
- **Subject = user id (as string), not email.** Email is mutable; if it were the subject, a profile
  email change would invalidate a live token or, worse, resolve to the wrong user. Put `email` and
  `role` in as extra claims for convenience.
- Claims: `sub` (user id), `email`, `role`, `iat`, `exp`.
- Expiry 24h. No refresh tokens — out of scope.

### 6.2 Filter chain

`JwtAuthenticationFilter extends OncePerRequestFilter`, registered before
`UsernamePasswordAuthenticationFilter`:

1. Read `Authorization: Bearer <token>`; if absent → `chain.doFilter`, continue unauthenticated.
2. Validate signature and expiry. Malformed/expired → clear context, continue (the entry point
   produces the 401).
3. Load the user by id, verify `status == ACTIVE`, set the `Authentication` in the
   `SecurityContextHolder` with authority `ROLE_USER` / `ROLE_ADMIN`.

### 6.3 `SecurityConfig`

```java
http
  .csrf(csrf -> csrf.disable())                      // stateless JWT API, no cookies
  .cors(Customizer.withDefaults())
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
      .requestMatchers("/api/admin/**").hasRole("ADMIN")
      .anyRequest().authenticated())
  .exceptionHandling(e -> e
      .authenticationEntryPoint(jwtAuthEntryPoint)   // 401
      .accessDeniedHandler(jwtAccessDeniedHandler))  // 403
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

`PasswordEncoder` bean = `new BCryptPasswordEncoder(10)`.

CORS: allow origin `http://localhost:5173`, methods `GET,POST,PUT,DELETE,OPTIONS`, header
`Authorization`, `Content-Type`.

Without the two custom handlers, an unauthenticated call returns 403 instead of 401 and the frontend
cannot tell "log in" from "not allowed" — wire them up.

### 6.4 Resolving the current user

Never accept a user id, account number, or email from the request body to identify the *caller*.
Always resolve from the token:

```java
Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
```

A `@AuthenticationPrincipal` custom principal is fine too. The rule: the caller's identity comes from
the token, never from the payload. Otherwise anyone can deposit into someone else's account.

---

## 7. Response Envelope

Every endpoint returns the same shape, so the frontend has one unwrap path.

Success:
```json
{ "success": true, "message": "Login successful", "data": { }, "timestamp": "2026-08-10T20:42:11Z" }
```

Error:
```json
{
  "success": false,
  "message": "Insufficient balance",
  "errorCode": "INSUFFICIENT_BALANCE",
  "errors": { "amount": "must be greater than zero" },
  "timestamp": "2026-08-10T20:42:11Z",
  "path": "/api/transactions/withdraw"
}
```

`errors` is present only for field-validation failures (`MethodArgumentNotValidException`).

---

## 8. API Contracts

All paths below are prefixed by `http://localhost:8080`. Everything except `/api/auth/**` requires
`Authorization: Bearer <token>`.

### Module 1 — Auth

**`POST /api/auth/register`**
```json
{ "username": "sidharth", "email": "sid@mail.com", "password": "Secret@123" }
```
Validation: username 3–20 chars `^[a-zA-Z0-9._]+$`; valid email; password 8–64 chars with at least
one letter and one digit. 201 on success. Returns the created profile + account details (no token —
the user then logs in). Duplicate username or email → 409 `DUPLICATE_RESOURCE`.

Flow: validate → check username unique → check email unique → BCrypt hash → save `User` → generate
account number → generate UPI ID → generate random balance → save `BankAccount` → return. The whole
method is `@Transactional`, so a failure mid-way leaves no orphan user.

**`POST /api/auth/login`**
```json
{ "email": "sid@mail.com", "password": "Secret@123" }
```
→ 200 `{ "token": "...", "tokenType": "Bearer", "expiresIn": 86400, "user": { id, username, email, role } }`

Bad email *or* bad password → 401 `INVALID_CREDENTIALS` with the identical message
("Invalid email or password"). Do not reveal which one was wrong. `status == DEACTIVATED` → 403
`ACCOUNT_DEACTIVATED`.

### Module 2 — Dashboard & Profile

**`GET /api/user/dashboard`**
```json
{
  "username": "sidharth", "email": "sid@mail.com",
  "accountNumber": "8421903357", "upiId": "sidharth@obs",
  "balance": 152340.00, "accountCreatedAt": "2026-07-02T11:20:00Z",
  "totalIncome": 21230.00, "totalExpense": 8140.50,
  "recentTransactions": [ /* latest 5 TransactionResponse */ ]
}
```
`totalIncome` = sum of deposits + incoming transfers for the current month.
`totalExpense` = withdrawals + outgoing transfers for the current month. Compute with a repository
aggregate query, not by loading all rows into memory.

**`GET /api/user/profile`** → `{ id, username, email, role, accountNumber, upiId, createdAt }`

**`PUT /api/user/profile`** → `{ "username": "...", "email": "..." }`
Both optional; ignore nulls. Uniqueness re-checked against *other* users. UPI ID stays unchanged.
Returns the updated profile.

**`PUT /api/user/change-password`** → `{ "currentPassword": "...", "newPassword": "..." }`
Verify current password first → 401 `INVALID_CREDENTIALS` if wrong. New password must differ from
current and satisfy the same policy. Returns a message only.

### Modules 3 & 4 — Deposit / Withdraw / Transfer

**`POST /api/transactions/deposit`** → `{ "amount": 10000.00, "description": "Salary" }`
**`POST /api/transactions/withdraw`** → same body.

Validation on `amount`: `@NotNull`, `@DecimalMin("0.01")`, `@Digits(integer=17, fraction=2)`.
Withdraw with `balance < amount` → 400 `INSUFFICIENT_BALANCE`, no rows written, balance untouched.

**`POST /api/transactions/transfer`**
```json
{ "receiverAccountNumber": "9876543210", "amount": 5000.00, "description": "Rent" }
```
or
```json
{ "receiverUpiId": "rahul@obs", "amount": 5000.00, "description": "Rent" }
```

Exactly one of `receiverAccountNumber` / `receiverUpiId` must be present — reject both-null and
both-present with 400 `INVALID_TRANSACTION`. Use a class-level `@AssertTrue` validator.

Ordered checks, in this order:
1. amount > 0
2. receiver exists → 404 `ACCOUNT_NOT_FOUND`
3. receiver ≠ sender → 400 `SELF_TRANSFER_NOT_ALLOWED`
4. both accounts `ACTIVE` → 400 `ACCOUNT_INACTIVE`
5. sender balance ≥ amount → 400 `INSUFFICIENT_BALANCE`
6. debit sender, credit receiver, write both ledger rows, commit

The service method carries `@Transactional(rollbackFor = Exception.class)`. Any failure rolls back
both sides — never debit without the matching credit.

**Deadlock note.** Two users transferring to each other at the same moment can deadlock if each locks
its own row first. Lock the two accounts in a deterministic order (ascending `id`) using
`@Lock(LockModeType.PESSIMISTIC_WRITE)` on a `findByIdForUpdate` repository method. Mention this in
your report — it is exactly the kind of detail that earns marks.

### Module 5 — History

**`GET /api/transactions?type=ALL&from=2026-08-01&to=2026-08-31&page=0&size=10`**

- `type` ∈ `ALL | DEPOSIT | WITHDRAW | TRANSFER`, default `ALL`
- `from` / `to` optional ISO dates, inclusive
- Sorted `createdAt DESC`
- Paginated. Response:

```json
{
  "content": [ {
      "transactionReference": "TXN1002345612",
      "type": "TRANSFER", "direction": "DEBIT",
      "amount": 5000.00,
      "senderAccountNumber": "8421903357",
      "receiverAccountNumber": "9876543210",
      "counterpartyName": "rahul",
      "status": "SUCCESS", "description": "Rent",
      "balanceAfterTransaction": 147340.00,
      "createdAt": "2026-08-10T20:42:11Z"
  } ],
  "page": 0, "size": 10, "totalElements": 42, "totalPages": 5, "last": false
}
```

`direction` is computed per viewer (`CREDIT` for deposits and incoming transfers, `DEBIT` otherwise)
so the UI can colour the row without re-deriving the logic. `counterpartyName` makes the list
readable — resolve it in the service, do not let the frontend do N lookups.

**`GET /api/transactions/{reference}`** → single transaction, 404 if it is not the caller's.

### Module 6 — UPI

**`GET /api/upi/me`** → `{ "upiId": "sidharth@obs" }`

**`GET /api/upi/resolve?upiId=rahul@obs`** →
`{ "upiId": "rahul@obs", "accountHolderName": "rahul", "valid": true }`

Used by the frontend to confirm the payee before sending. Return the name only — never the account
number or balance of another user. Unknown UPI ID → 404 `ACCOUNT_NOT_FOUND`.

Transfers themselves go through `/api/transactions/transfer` with `receiverUpiId`. There is no
separate UPI transfer endpoint — one code path, two lookup strategies.

Validate UPI format with `^[a-z0-9._]{3,20}@obs$`.

### Module 7 — Admin (`ROLE_ADMIN` only)

| Endpoint | Returns |
|---|---|
| `GET /api/admin/stats` | `{ totalUsers, totalAccounts, totalTransactions, totalMoneyInSystem, activeUsers, deactivatedUsers }` |
| `GET /api/admin/users?search=&page=&size=` | paginated users (never any password field) |
| `GET /api/admin/users/{id}` | user + account details |
| `PUT /api/admin/users/{id}/status` | body `{ "status": "DEACTIVATED" }` |
| `GET /api/admin/accounts?search=&page=&size=` | paginated accounts |
| `GET /api/admin/transactions?type=&status=&search=&page=&size=` | paginated, all users |

`totalMoneyInSystem` = `SELECT COALESCE(SUM(balance),0) FROM bank_accounts`.

A deactivated user cannot log in and cannot send or receive money. An admin cannot deactivate their
own account — 400 `INVALID_TRANSACTION`.

Seed one admin on startup via a `CommandLineRunner` guarded by "does an admin already exist?".
Credentials from env vars, and print a warning if the defaults are still in use.

### Module 8 — Notifications (optional)

`GET /api/notifications` · `PUT /api/notifications/{id}/read` · `DELETE /api/notifications/{id}`

Written by `NotificationService` after a successful deposit / withdraw / transfer. Skip this module
entirely if time is short — nothing else depends on it.

---

## 9. Exceptions & Error Codes

`GlobalExceptionHandler` annotated `@RestControllerAdvice`. One handler per exception, all returning
the error envelope from §7.

| Exception | HTTP | `errorCode` |
|---|---|---|
| `UserNotFoundException` | 404 | `USER_NOT_FOUND` |
| `AccountNotFoundException` | 404 | `ACCOUNT_NOT_FOUND` |
| `DuplicateResourceException` | 409 | `DUPLICATE_RESOURCE` |
| `InvalidCredentialsException` | 401 | `INVALID_CREDENTIALS` |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` |
| `AccessDeniedException` (Spring) | 403 | `FORBIDDEN` |
| `InsufficientBalanceException` | 400 | `INSUFFICIENT_BALANCE` |
| `InvalidTransactionException` | 400 | `INVALID_TRANSACTION` |
| `TransactionFailedException` | 500 | `TRANSACTION_FAILED` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` (+ `errors` map) |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` |

The catch-all logs the full stack trace server-side and returns a generic message. Never leak stack
traces, SQL, or class names to the client.

Build these handlers in Module 1 and extend them as you go — do not defer Module 9 to the end.

---

## 10. Build Order & Definition of Done

Work strictly in this order; each step must be Postman-tested before starting the next.

| # | Module | Done when |
|---|---|---|
| 1 | Auth + account creation | Register creates user + account with number, UPI, balance; login returns a working JWT; a protected endpoint rejects a missing/expired token with 401 |
| 2 | Dashboard + profile | All four user endpoints return correct data for the token holder only |
| 3 | Deposit + withdraw | Balance moves correctly; overdraft rejected; a transaction row exists for each success |
| 4 | Transfer | Both balances move atomically; killing the app mid-method (or a forced exception) leaves both balances unchanged |
| 5 | History | Pagination + all four filters correct; each side of a transfer sees the right direction and balance |
| 6 | UPI | Resolve returns the payee name; transfer by UPI works; invalid format rejected |
| 7 | Admin | Stats correct; a `USER` token gets 403 on every `/api/admin/**` route |
| 9 | Validation + exceptions | Every error path returns the standard envelope — no raw Spring error JSON anywhere |
| 10 | Swagger + Postman | Swagger UI lists every endpoint; Postman collection covers success and failure per endpoint |
| 8 | Notifications | Optional — only if time remains |

---

## 11. Test Matrix (Postman)

Save these as a collection with an environment variable `{{token}}` set by a login test script.

**Must-pass failure cases** — these are what a demo/viva gets probed on:

- register with a duplicate email → 409
- register with a duplicate username → 409
- register with a 5-char password → 400 with `errors.password`
- login with a wrong password → 401, message identical to unknown-email case
- any protected endpoint with no header → 401
- any protected endpoint with a tampered token → 401
- deposit `0` and `-500` → 400
- withdraw more than the balance → 400, balance unchanged (verify via dashboard)
- transfer to a non-existent account number → 404
- transfer to a non-existent UPI ID → 404
- transfer to your own account number → 400
- transfer with both `receiverAccountNumber` and `receiverUpiId` → 400
- `USER` token on `/api/admin/stats` → 403
- deactivated user login → 403

**Concurrency check (worth demoing):** fire two simultaneous withdrawals of the full balance from the
same account. Exactly one must succeed. If both succeed, your locking is wrong.

---

## 12. Things That Commonly Break This Project

- Regenerating the account number or balance on login. Login is read-only. Assert this with a test.
- Storing money as `double` — `0.1 + 0.2` will embarrass you in the viva.
- `@Transactional` on a private method or a method called from within the same class — Spring's proxy
  will not apply it and the transfer silently loses atomicity. Keep the annotation on a public
  service method called from another bean.
- Returning the `User` entity from a controller, dragging the password hash and lazy proxies along.
- Trusting an account number from the request body to identify the sender.
- Forgetting `spring.jpa.open-in-view=false`, then hitting `LazyInitializationException` only after
  the frontend is wired up.
- Committing the JWT secret or DB password to Git. Use env vars; add a `.gitignore`.
