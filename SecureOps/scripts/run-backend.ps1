# Starts the SecureOps backend on http://localhost:8080
. "$PSScriptRoot\load-env.ps1"
Set-Location "$PSScriptRoot\..\backend"
mvn spring-boot:run
