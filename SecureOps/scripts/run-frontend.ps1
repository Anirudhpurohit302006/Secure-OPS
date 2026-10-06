# Starts the SecureOps frontend on http://localhost:5173 (proxies /api to the backend)
Set-Location "$PSScriptRoot\..\frontend"
if (-not (Test-Path node_modules)) { npm install }
npm run dev
