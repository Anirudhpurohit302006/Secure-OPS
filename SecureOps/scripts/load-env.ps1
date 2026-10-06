# Loads KEY=VALUE lines from config/secureops.env into the current PowerShell session.
# Secrets live only in that git-ignored file and in process memory - never in the repo.
param([string]$EnvFile = (Join-Path $PSScriptRoot '..\config\secureops.env'))
if (-not (Test-Path $EnvFile)) { Write-Error "Missing $EnvFile. Copy config\secureops.example.env to config\secureops.env and edit it."; exit 1 }
Get-Content $EnvFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -eq '' -or $line.StartsWith('#')) { return }
  $i = $line.IndexOf('=')
  if ($i -gt 0) { [Environment]::SetEnvironmentVariable($line.Substring(0, $i).Trim(), $line.Substring($i + 1).Trim(), 'Process') }
}
Write-Host "Loaded environment from $EnvFile"
