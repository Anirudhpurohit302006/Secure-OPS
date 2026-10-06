# SecureOps - PostgreSQL database setup (Windows PowerShell 5.1+ / PowerShell 7).
# 1) finds psql  2) verifies the server  3) creates role + database if missing  4) applies schema.sql  5) applies seed.sql
# Reads DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD from config\secureops.env (git-ignored) if it exists.
# Asks for the PostgreSQL administrator ("postgres") password; that password is used only for this script.
param(
  [string]$AdminUser = 'postgres',
  [switch]$SkipSeed
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repo 'config\secureops.env'
if (Test-Path $envFile) { . "$PSScriptRoot\load-env.ps1" -EnvFile $envFile }

function Get-Setting($name, $default) { $v = [Environment]::GetEnvironmentVariable($name, 'Process'); if ([string]::IsNullOrWhiteSpace($v)) { $default } else { $v } }
$dbHost = Get-Setting 'DB_HOST' 'localhost'
$dbPort = Get-Setting 'DB_PORT' '5432'
$dbName = Get-Setting 'DB_NAME' 'secureops'
$dbUser = Get-Setting 'DB_USER' 'secureops'
$dbPass = Get-Setting 'DB_PASSWORD' ''

function Fail($msg) { Write-Host "FAILED: $msg" -ForegroundColor Red; exit 1 }
function Ok($msg)   { Write-Host "OK  $msg" -ForegroundColor Green }

# identifiers are interpolated into SQL, so only plain lowercase names are accepted
if ($dbName -notmatch '^[a-z_][a-z0-9_]{0,62}$') { Fail "DB_NAME '$dbName' must be lowercase letters, digits or underscore." }
if ($dbUser -notmatch '^[a-z_][a-z0-9_]{0,62}$') { Fail "DB_USER '$dbUser' must be lowercase letters, digits or underscore." }
if ([string]::IsNullOrWhiteSpace($dbPass) -or $dbPass -eq 'CHANGE_ME') { Fail "Set a real DB_PASSWORD in config\secureops.env first (copy config\secureops.example.env)." }

# 1. psql available?
$psql = (Get-Command psql -ErrorAction SilentlyContinue).Source
if (-not $psql) {
  $psql = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\psql.exe' -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $psql) { Fail "psql.exe not found. Install PostgreSQL (see documentation\SETUP.md) or add its bin folder to PATH." }
Ok "psql found: $psql"
& $psql --version

# 2. server reachable with the administrator account?
$adminSecure = Read-Host "Password for PostgreSQL user '$AdminUser'" -AsSecureString
$env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($adminSecure))
$common = @('-h', $dbHost, '-p', $dbPort, '-v', 'ON_ERROR_STOP=1', '-X', '-q')
try {
  $ver = & $psql @common -U $AdminUser -d postgres -tAc 'SHOW server_version' 2>&1
  if ($LASTEXITCODE -ne 0) { throw "$ver" }
  Ok "Connected to PostgreSQL $ver on ${dbHost}:${dbPort}"
} catch { Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue; Fail "Cannot connect: $_`n  Is the 'postgresql-x64-*' Windows service running? Is the password right?" }

try {
  # 3a. application role (password passed as a psql variable, never on the command line)
  $roleExists = & $psql @common -U $AdminUser -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname = '$dbUser'"
  $roleSql = if ($roleExists -match '1') { "ALTER ROLE $dbUser LOGIN PASSWORD :'pw';" } else { "CREATE ROLE $dbUser LOGIN PASSWORD :'pw';" }
  $roleSql | & $psql @common -U $AdminUser -d postgres -v "pw=$dbPass" -f - | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "could not create/update role $dbUser" }
  Ok "Role '$dbUser' ready"

  # 3b. database (CREATE DATABASE cannot run in a transaction, so it is its own call; connect to the default 'postgres' DB first)
  $dbExists = & $psql @common -U $AdminUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$dbName'"
  if ($dbExists -match '1') { Ok "Database '$dbName' already exists" }
  else {
    & $psql @common -U $AdminUser -d postgres -c "CREATE DATABASE $dbName OWNER $dbUser"
    if ($LASTEXITCODE -ne 0) { throw "could not create database $dbName" }
    Ok "Database '$dbName' created"
  }
} catch { Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue; Fail "$_" }

# 4/5. schema + seed run as the application role (so it owns the tables)
$env:PGPASSWORD = $dbPass
try {
  & $psql @common -U $dbUser -d $dbName -f (Join-Path $repo 'database\schema.sql') 2>&1 | Where-Object { $_ -notmatch 'NOTICE' }
  if ($LASTEXITCODE -ne 0) { throw "schema.sql failed" }
  Ok "schema.sql applied"
  if (-not $SkipSeed) {
    & $psql @common -U $dbUser -d $dbName -f (Join-Path $repo 'database\seed.sql')
    if ($LASTEXITCODE -ne 0) { throw "seed.sql failed" }
    Ok "seed.sql applied (RHEL-01 192.168.159.129, RHEL-02 192.168.159.128)"
  }
  Write-Host "`nTargets now in the database:"
  & $psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -X -c "SELECT machine_id, machine_name, ip_address, ssh_username FROM machines ORDER BY machine_id"
} catch { Fail "$_" } finally { Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue }

Write-Host "`nDatabase setup complete. Note: seeded ssh_username is CHANGE_ME - set the real user under Settings -> Edit." -ForegroundColor Green
