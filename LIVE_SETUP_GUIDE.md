# Live setup checklist — SecureLog

This guide configures the included Java 21 + Spring Boot web dashboard and Swing desktop companion with Supabase and Algorand TestNet.

## 1. Create a Supabase project

1. Open https://supabase.com/dashboard and sign in.
2. Create a new project. Save the database password somewhere private.
3. Open SQL Editor → New query.
4. Paste and run the complete contents of `supabase/schema.sql` from this project.
5. Open Project Settings → API Keys (or the project's Connect dialog).
6. Copy the Project URL and create/copy a **Secret key** (`sb_secret_...`). The legacy `service_role` key also works if that is what your project currently exposes.
7. Keep the secret key private. It belongs only in the Java backend environment, never in the browser, Swing app, Git repository, or chat.

## 2. Current project setting: Demo Account

The project defaults to `ALGORAND_DEMO_MODE=true` and uses a placeholder named **SecureLog Demo Account**. It is not a real Algorand account, has no address or private key, and cannot submit transactions or receive ALGO. The app will label records `DEMO_ONLY`; verification checks the stored SHA-256 digest only and will not claim blockchain confirmation. No wallet setup is needed to try the demo.

## 3. Optional: create a real Algorand TestNet account

1. Use a wallet that supports Algorand TestNet, or create a dedicated development account using the official Algorand account guidance: https://dev.algorand.co/concepts/accounts/creating-accounts/ .
2. Select **TestNet**, not MainNet.
3. Back up the account recovery phrase offline. Never share it with anyone or paste it into chat.
4. Copy only the public account address.
5. Fund the public address with test ALGO using the official funding guide: https://dev.algorand.co/concepts/accounts/funding/ . Follow the TestNet faucet / dispenser instructions there.
6. Check the address on the TestNet explorer (Lora): https://lora.algokit.io/testnet . Confirm it has a balance before enabling writes.

## 4. Configure the Java backend

The app reads environment variables; Spring Boot does not automatically load `.env` in this starter. Copy `.env.example` to `.env` for reference, but export the variables in your terminal or add them to your IDE Run Configuration.

Required values:

- `SUPABASE_URL`: your project URL, e.g. `https://<project-ref>.supabase.co`
- `SUPABASE_SECRET_KEY`: your Supabase secret key (or legacy service_role key)
- `ALGORAND_ALGOD_URL`: `https://testnet-api.algonode.cloud`
- `ALGORAND_INDEXER_URL`: `https://testnet-idx.algonode.cloud`
- `ALGORAND_ALGOD_TOKEN`: leave blank for the public AlgoNode endpoint unless your provider requires a token
- `ALGORAND_DEMO_MODE`: keep `true` to use the placeholder account; set `false` only for a real TestNet wallet
- `ALGORAND_MNEMONIC`: recovery phrase for the dedicated TestNet-only account (leave blank in demo mode)
- `BLOCKCHAIN_WRITES_ENABLED`: keep `false` until the health endpoint confirms node connectivity, Supabase is configured, and the TestNet account is funded; then set `true`

### Windows PowerShell

```powershell
$env:SUPABASE_URL="https://YOUR_PROJECT_REF.supabase.co"
$env:SUPABASE_SECRET_KEY="sb_secret_REPLACE_ME"
$env:ALGORAND_ALGOD_URL="https://testnet-api.algonode.cloud"
$env:ALGORAND_INDEXER_URL="https://testnet-idx.algonode.cloud"
$env:ALGORAND_ALGOD_TOKEN=""
$env:ALGORAND_DEMO_MODE="true"
$env:ALGORAND_MNEMONIC="YOUR TESTNET MNEMONIC"
$env:BLOCKCHAIN_WRITES_ENABLED="false"
mvn clean spring-boot:run
```

After verifying Supabase and Algorand health and confirming the TestNet balance, stop the app, set `$env:BLOCKCHAIN_WRITES_ENABLED="true"`, and restart it.

### macOS / Linux

```bash
export SUPABASE_URL="https://YOUR_PROJECT_REF.supabase.co"
export SUPABASE_SECRET_KEY="sb_secret_REPLACE_ME"
export ALGORAND_ALGOD_URL="https://testnet-api.algonode.cloud"
export ALGORAND_INDEXER_URL="https://testnet-idx.algonode.cloud"
export ALGORAND_ALGOD_TOKEN=""
export ALGORAND_DEMO_MODE="true"
export ALGORAND_MNEMONIC="YOUR TESTNET MNEMONIC"
export BLOCKCHAIN_WRITES_ENABLED="false"
mvn clean spring-boot:run
```

Do not put real values into source files or commit them. For an IDE, add these as Run Configuration environment variables instead of hardcoding them.

## 5. Start and test the website

Open http://localhost:8080 . Then check:

- `GET http://localhost:8080/api/health` — app and Algorand node status
- `GET http://localhost:8080/api/logs?limit=20` — recent rows

Create a demo event:

```bash
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{"severity":"HIGH","source":"auth-service","eventType":"LOGIN_FAILURE","message":"Repeated login failures","actor":"demo-user","ipAddress":"192.0.2.10"}'
```

On Windows PowerShell, use `curl.exe` or Postman if the `curl` alias causes problems.

When blockchain writes are disabled, the record should still be saved in Supabase with a pending/configuration chain status. When enabled and funded, the response should include a transaction ID. Allow a short indexing delay before checking verification.

## 6. Start the Swing desktop companion

Keep Spring Boot running. In a second terminal, from the project root:

```bash
mvn -q -DskipTests package
java -cp "target/classes;target/dependency/*" com.example.logintegrity.desktop.SwingConsole
```

On macOS/Linux replace the semicolon with a colon:

```bash
java -cp "target/classes:target/dependency/*" com.example.logintegrity.desktop.SwingConsole
```

The Swing client calls the local Spring Boot API; it does not need Supabase keys or the Algorand mnemonic.

## 7. Troubleshooting

- **Supabase 401/403:** confirm the project URL and server-side secret/service_role key are correct. Never use the publishable key as the backend secret key.
- **Supabase relation/table error:** run `supabase/schema.sql` in the correct project's SQL Editor.
- **Algorand connection false:** confirm internet access and the TestNet endpoints; check provider token requirements if using a different node provider.
- **Transaction rejected / insufficient balance:** confirm the mnemonic belongs to the TestNet address you funded and that it has enough test ALGO for fees and minimum balance.
- **Transaction not found during verification:** wait briefly for the TestNet Indexer to catch up, then retry verification.
- **Port 8080 busy:** set `SERVER_PORT=8081` and open `http://localhost:8081`.

## Safety and project limitations

- Never use a MainNet mnemonic in this project.
- Never send your recovery phrase, Supabase secret key, database password, or API tokens in ChatGPT, email, screenshots, or public repositories.
- This is an academic starter, not a production-ready security platform. Add user authentication, authorization/RBAC, rate limiting, hardened input validation, secret management, monitoring, confirmation-round handling, and robust retry/reconciliation before exposing it publicly.
- The current starter anchors the SHA-256 digest in a zero-value self-payment note. It does not store full logs on-chain.
