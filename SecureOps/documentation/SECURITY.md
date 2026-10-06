# Security considerations

- **No arbitrary execution.** Only `ApprovedCommand` constants run on targets; there is no endpoint that accepts a command, no terminal.
- **Credentials never reach the browser, Git or PostgreSQL.** SSH key path/passphrase and the DB password come from environment variables (`config/secureops.env`, git-ignored). The UI has no password field for targets.
- **Passwords**: the admin password is stored only as a BCrypt hash. Login uses constant-work comparison, session-ID rotation on login, HttpOnly + SameSite=Lax cookie, 8 h idle timeout, and a per-IP lockout.
- **Authorization boundary**: a server-side interceptor guards every `/api/**` route except login/logout.
- **SQL**: prepared statements only; input validated (names, IPv4, usernames, ports, ranges).
- **Errors**: unexpected exceptions are logged server-side and returned as a generic message.
- **Host keys**: set `SSH_KNOWN_HOSTS_PATH` (after one manual `ssh user@ip`) to enforce strict host-key checking. Without it the backend logs a warning and does not verify host keys.
- **Least privilege**: non-root SSH user; two exact sudo rules for two root-only files (optional).
- **CORS/CSRF**: only `CORS_ALLOWED_ORIGINS` (default http://localhost:5173) is allowed; JSON bodies plus SameSite cookies protect state-changing calls. In development the Vite proxy makes the app same-origin anyway.
- **Transport**: V1 serves HTTP on localhost for a single-operator lab. Put it behind HTTPS (reverse proxy) before exposing it to a network.
- The application connects to PostgreSQL with its own role (`DB_USER`) that owns only the `secureops` database; use a strong `DB_PASSWORD`, keep PostgreSQL listening on localhost, and never expose port 5432 to the network. The password exists only in `config/secureops.env` (git-ignored) and the backend process; the browser never talks to PostgreSQL.
