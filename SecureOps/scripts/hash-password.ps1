# Generates the BCrypt hash for ADMIN_PASSWORD_HASH. The password is passed via an environment variable, not the command line.
$secure = Read-Host "New SecureOps admin password (min 8 chars)" -AsSecureString
$env:SECUREOPS_NEW_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
Set-Location "$PSScriptRoot\..\backend"
mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.secureops.util.PasswordHashTool"
Remove-Item Env:\SECUREOPS_NEW_PASSWORD
Write-Host "`nPut the line above into config\secureops.env as ADMIN_PASSWORD_HASH=<hash>"
