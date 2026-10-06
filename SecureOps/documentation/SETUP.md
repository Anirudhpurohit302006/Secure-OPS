# Setup guide (Windows 11, PostgreSQL)

## 1. Prerequisites
Java JDK 21 or newer (25 LTS is fine) · Maven 3.9+ (`mvn -v`) · Node.js 20+ · **PostgreSQL 16 or newer** · Git · VMware Workstation · VS Code.
No Docker, WSL or Linux is needed on the Windows side. The two RHEL VMs must be reachable (RHEL-01 192.168.159.129, RHEL-02 192.168.159.128) with `sshd` running.

## 2. Install PostgreSQL (Windows)
1. Download the Windows installer (EDB) for **PostgreSQL 16 or 17** from https://www.postgresql.org/download/windows/ and run it.
2. Components: keep **PostgreSQL Server** and **Command Line Tools** (this provides `psql`); pgAdmin is optional; Stack Builder is not needed.
3. Accept the default data directory and port **5432**. Choose a password for the `postgres` administrator and remember it.
4. The installer registers a normal Windows service named `postgresql-x64-16` (or `-17`) that starts automatically.
5. Check it (PowerShell): `Get-Service postgresql*` should show **Running**. If not, start it as Administrator: `Start-Service postgresql-x64-16`.
6. Optional: add `C:\Program Files\PostgreSQL\16\bin` to PATH. The setup script also finds `psql.exe` there on its own.
7. Check: `psql -U postgres -h localhost -c "SELECT version();"` (asks for the administrator password).

## 3. Configure SecureOps (environment variables)
```powershell
copy config\secureops.example.env config\secureops.env
notepad config\secureops.env
```
| Variable | Meaning | Example |
|---|---|---|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | database name | `secureops` |
| `DB_USER` | application role (created by the setup script) | `secureops` |
| `DB_PASSWORD` | password for that role - choose a new strong one | `CHANGE_ME` |
| `SSH_PRIVATE_KEY_PATH` | private key used for RHEL (stays outside the repo) | `C:/Users/you/.ssh/secureops_ed25519` |
| `SSH_PRIVATE_KEY_PASSPHRASE` | key passphrase, if any | |
| `SSH_PASSWORD` | development fallback instead of a key (environment only) | |
| `SSH_KNOWN_HOSTS_PATH` | enables strict host-key checking | `C:/Users/you/.ssh/known_hosts` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD_HASH` | SecureOps login (BCrypt hash) | from `scripts\hash-password.ps1` |
| `COLLECTION_INTERVAL_MINUTES` | 30 normally; values above 45 are refused | `30` |

Generate the admin hash: `scripts\hash-password.ps1` and paste the output as `ADMIN_PASSWORD_HASH=...`.
`config\secureops.env` is git-ignored; never commit it.

## 4. Create the database
```powershell
scripts\setup-database.ps1
```
It asks for the `postgres` administrator password and then: finds `psql`, verifies the connection, creates the `DB_USER` role and the `DB_NAME`
database if missing (connecting to the default `postgres` database first), applies `database\schema.sql`, applies `database\seed.sql`
(RHEL-01 = 192.168.159.129, RHEL-02 = 192.168.159.128) and prints the result. It is safe to run again. Use `-SkipSeed` to skip the seed.
Manual equivalent:
```powershell
psql -U postgres -h localhost -c "CREATE ROLE secureops LOGIN PASSWORD '<DB_PASSWORD>'"
psql -U postgres -h localhost -c "CREATE DATABASE secureops OWNER secureops"
psql -U secureops -h localhost -d secureops -f database\schema.sql
psql -U secureops -h localhost -d secureops -f database\seed.sql
```

## 5. SSH keys (Windows -> RHEL)
```powershell
ssh-keygen -t ed25519 -f $env:USERPROFILE\.ssh\secureops_ed25519
type $env:USERPROFILE\.ssh\secureops_ed25519.pub | ssh <user>@192.168.159.129 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys"
# repeat for 192.168.159.128
ssh -i $env:USERPROFILE\.ssh\secureops_ed25519 <user>@192.168.159.129      # accept the host key once -> known_hosts
```
The private key stays in `~/.ssh`; it is never copied into the project or stored in PostgreSQL.

## 6. RHEL preparation (login history and SSH checks)
Copy `scripts/rhel-setup.sh` to each VM and run `sudo ./rhel-setup.sh <ssh-username>`. It adds two exact sudo rules so that user can read
`/var/log/secure` (last 5000 lines) and `/etc/ssh/sshd_config`, nothing else (see COLLECTORS.md). Optional: `sudo dnf install httpd && sudo systemctl enable --now httpd`.

## 7. Start SecureOps
```powershell
scripts\run-backend.ps1        # http://localhost:8080   (loads config\secureops.env, runs mvn spring-boot:run)
scripts\run-frontend.ps1       # http://localhost:5173   (second terminal)
```

## 8. Configure targets, test SSH, first collection
1. Open http://localhost:5173 and sign in with `ADMIN_USERNAME` and the password you hashed.
2. **Settings**: RHEL-01 and RHEL-02 already exist from the seed. Press **Edit** and set the real SSH username (and confirm the IPs).
3. Press **Test Connection** for each. Success shows "Connected to rhel01/rhel02" and the status turns Online.
4. Press **Collect now** for each (the scheduler also runs ~20 s after backend start, then every 30 minutes).
5. Check **Home**, **Systems**, **Security** and **Analytics**. Network charts need two collections.
6. Optional check in PostgreSQL: `psql -U secureops -h localhost -d secureops -c "SELECT machine_id, timestamp, cpu_usage FROM system_metrics ORDER BY 2 DESC LIMIT 5"`.

## 9. Tests
```powershell
cd backend;  mvn clean test    # unit tests need no database and no RHEL
cd frontend; npm test
cd frontend; npm run build     # type-check + production build
```
PostgreSQL integration test: see `tests/README.md` (skipped unless `SECUREOPS_IT_DB_URL` is set).

## 10. Troubleshooting
| Symptom | Likely cause / fix |
|---|---|
| `setup-database.ps1`: "psql.exe not found" | Install the **Command Line Tools** component or add `C:\Program Files\PostgreSQL\16\bin` to PATH. |
| "Cannot connect" in the setup script | Service not running (`Get-Service postgresql*`, `Start-Service ...`), wrong `postgres` password, or non-default port (`DB_PORT`). |
| Backend: `password authentication failed for user "secureops"` | `DB_PASSWORD` differs from the one used when the role was created: re-run `scripts\setup-database.ps1` (it updates the role password). |
| Backend: `database "secureops" does not exist` / `relation "machines" does not exist` | Run `scripts\setup-database.ps1`. |
| Backend: `Connection refused` to port 5432 | PostgreSQL service stopped or `DB_HOST`/`DB_PORT` wrong. |
| Login says "Admin credentials are not configured" | `ADMIN_PASSWORD_HASH` empty. Run `scripts\hash-password.ps1`. |
| Test Connection: "authentication failed" | Wrong `SSH_PRIVATE_KEY_PATH`, key not in `authorized_keys`, or wrong SSH username. |
| "Host key verification failed" | `known_hosts` has no/other entry: run `ssh user@ip` once, or remove the stale line after reinstalling a VM. |
| "Connection timed out" | VM off, wrong IP, or VMware network/firewall issue (`ping 192.168.159.129`). |
| Login history empty | Run `scripts/rhel-setup.sh <user>` on the VM. |
| httpd shows Stopped | `sudo dnf install httpd && sudo systemctl enable --now httpd`. |
| Charts say "Waiting for first collection" | Press **Collect now** in Settings, or wait for the first scheduled cycle. |
| Frontend: "Cannot reach the SecureOps backend" | Backend not running on 8080 (the Vite dev server proxies `/api`). |
| Times look off | UTC is stored; the browser converts to local time. |
