# SecureLog — Decentralized Security Log Integrity

A Java 21 + Spring Boot application with a responsive web dashboard, optional Java Swing desktop console, Supabase persistence, SHA-256 log fingerprints, and Algorand TestNet anchoring.

## What this project does

1. Accepts security events through `POST /api/logs`.
2. Builds a canonical event string and computes SHA-256.
3. Optionally submits a zero-ALGO self-payment on Algorand TestNet with `SECLOG-SHA256:<digest>` in the transaction note.
4. Stores the event, digest, and transaction ID in Supabase using its REST Data API.
5. Verifies an event by recomputing its digest from the database row and comparing the digest with the note returned by the Algorand TestNet Indexer.
6. Shows health, event counts, recent events, and verification controls in the web dashboard.

The project now starts in **Demo Mode** (`ALGORAND_DEMO_MODE=true`). It uses a placeholder named **SecureLog Demo Account** only; this is not a real Algorand address or wallet. In this mode, logs are SHA-256 hashed and stored in Supabase, but no wallet is loaded, no transaction is signed, and nothing is written to Algorand. Verification reports `DEMO_VERIFIED` only when the stored log hash matches; it does not claim an on-chain proof.

To switch to live TestNet later, set `ALGORAND_DEMO_MODE=false`, configure your own dedicated funded TestNet mnemonic securely, and only then consider enabling writes. Never use a MainNet mnemonic.

## Technology

- Java 21, Spring Boot 3.5.7, Maven
- Algorand Java SDK (`com.algorand:algosdk`)
- Algorand TestNet algod + Indexer REST endpoints
- Supabase Postgres through the Supabase REST Data API
- HTML, CSS, vanilla JavaScript dashboard
- Java Swing desktop companion

## Prerequisites

- JDK 21+
- Maven 3.9+
- A Supabase project
- Optional: a dedicated Algorand TestNet account funded with TestNet ALGO

## Setup

### 1. Create Supabase table

Open your Supabase project's SQL Editor and run [`supabase/schema.sql`](supabase/schema.sql). This creates `public.security_logs`, indexes it, enables RLS, and revokes browser-role access. The Java backend accesses the table with a secret key, which must remain server-side.

### 2. Configure environment

Copy `.env.example` to `.env` and fill in your values. Spring Boot does not automatically load `.env`; export the variables in your terminal or configure them in your IDE run configuration.

Linux/macOS example:

```bash
export SUPABASE_URL="https://YOUR_PROJECT_REF.supabase.co"
export SUPABASE_SECRET_KEY="sb_secret_..."
export ALGORAND_ALGOD_URL="https://testnet-api.algonode.cloud"
export ALGORAND_INDEXER_URL="https://testnet-idx.algonode.cloud"
export ALGORAND_DEMO_MODE="true"
export BLOCKCHAIN_WRITES_ENABLED="false"
```

PowerShell example:

```powershell
$env:SUPABASE_URL="https://YOUR_PROJECT_REF.supabase.co"
$env:SUPABASE_SECRET_KEY="sb_secret_..."
$env:ALGORAND_ALGOD_URL="https://testnet-api.algonode.cloud"
$env:ALGORAND_INDEXER_URL="https://testnet-idx.algonode.cloud"
$env:BLOCKCHAIN_WRITES_ENABLED="false"
```

For actual on-chain anchoring later, fund a dedicated TestNet account, set `ALGORAND_DEMO_MODE="false"`, then set:

```bash
export ALGORAND_MNEMONIC="YOUR 25 WORD TESTNET MNEMONIC"
export BLOCKCHAIN_WRITES_ENABLED="true"
```

Never use a MainNet account or real funds. Never commit `.env`, mnemonics, Supabase secret keys, or provider API tokens. For production, use a secret manager and consider a separate signing service or HSM rather than keeping a mnemonic in the application process.

### 3. Run

```bash
mvn clean spring-boot:run
```

Open <http://localhost:8080>.

### 4. Run the Swing companion (optional)

Keep the Spring Boot server running, then in another terminal:

```bash
mvn -q -DskipTests package
java -cp "target/classes:target/dependency/*" com.example.logintegrity.desktop.SwingConsole
```

On Windows, use `;` instead of `:` in the classpath. If you prefer an IDE, run `com.example.logintegrity.desktop.SwingConsole` after starting the Spring Boot app. The Swing console uses the same backend API; it does not connect directly to Supabase or hold blockchain credentials.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Application, Supabase configuration, Algorand node status |
| GET | `/api/logs?limit=100` | Latest security events |
| POST | `/api/logs` | Hash, optionally anchor, and store an event |
| POST | `/api/verify/{id}` | Recompute digest and verify Algorand transaction note |

Example:

```bash
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{"severity":"HIGH","source":"auth-service","eventType":"LOGIN_FAILURE","message":"Repeated login failures","actor":"demo-user","ipAddress":"192.0.2.10"}'
```

## Integrity model and limitations

- SHA-256 is a tamper-evident fingerprint, not encryption. Do not put passwords, access tokens, personal data, or full sensitive log payloads in transaction notes.
- The transaction note contains only the digest. The full event is stored in Supabase.
- A transaction is first reported as `SUBMITTED`; this starter does not wait for a confirmed round. Verification uses the TestNet Indexer, so it may take a short time before the transaction is searchable.
- Verification checks the current database fields against the stored digest and then checks the on-chain note. It is a simple demonstrator; a production design should preserve a canonical payload/version, add signed ingestion identities, key rotation, rate limiting, authentication/RBAC, audit export, alerting, and stronger failure/retry handling.
- The canonical payload is stored alongside the digest so verification does not depend on timestamp or IP-address display formatting. The starter dashboard displays `VERIFIED` only from a verification response; it does not update `integrity_valid` in Supabase yet.
- The API is intended for local academic demonstration. Add authentication, authorization, CSRF/CORS policy, request-size limits, input sanitization, and operational monitoring before public deployment.

## Project layout

```text
src/main/java/com/example/logintegrity/
  LogIntegrityApplication.java
  controller/LogController.java
  model/SecurityLog.java
  service/AlgorandService.java
  service/HashService.java
  service/SupabaseService.java
  desktop/SwingConsole.java
src/main/resources/
  application.properties
  static/index.html
  static/styles.css
  static/app.js
supabase/schema.sql
```

## References

- [Algorand Java SDK](https://github.com/algorand/java-algorand-sdk)
- [Algorand SDK overview](https://dev.algorand.co/resources/sdk/sdk-list/)
- [Supabase Data REST API](https://supabase.com/docs/guides/api)
- [Supabase secure data](https://supabase.com/docs/guides/database/secure-data)
- [Supabase Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security)
