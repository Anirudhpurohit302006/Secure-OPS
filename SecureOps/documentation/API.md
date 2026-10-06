# REST API

JSON over HTTP. Everything except `POST /api/auth/login` and `/logout` needs the session cookie (`SECUREOPS_SESSION`).
Errors: `{ "error": "CODE", "message": "user-safe text" }` (never a stack trace). `target` = machine id or `all`; `range` = `6h|24h|7d|30d`.

| Method | Path | Notes |
|---|---|---|
| POST | /api/auth/login | `{username,password}` -> `{username}`; 5 failures lock the client IP for 5 minutes |
| POST | /api/auth/logout | 204 |
| GET | /api/auth/session | 200 or 401 |
| GET/POST | /api/targets | list / create `{machineName,ipAddress,sshUsername,sshPort?,enabled?}` |
| GET/PUT/DELETE | /api/targets/{id} | DELETE also deletes the target's history |
| POST | /api/targets/{id}/test | connection test (updates status) |
| POST | /api/targets/{id}/collect | run one collection now |
| GET | /api/systems, /api/systems/{id} | overview incl. latest metrics, services, security status |
| GET | /api/systems/{id}/metrics?range= | CPU/RAM/disk/uptime history |
| GET | /api/systems/{id}/network, /ports | interfaces + counters; listening ports |
| GET | /api/systems/{id}/services, /services/history?range= | |
| GET | /api/systems/{id}/security | controls, SSH config, ports, findings |
| GET | /api/systems/{id\|all}/auth-events?range=&type= | `type` = SSH_LOGIN_SUCCESS / SSH_LOGIN_FAILED |
| GET | /api/systems/{id\|all}/security-events?range= | |
| GET | /api/analytics/{cpu,memory,disk,network,services,authentication,security}?target=&range= | PostgreSQL history only |
| GET | /api/dashboard/summary?target=, /api/dashboard/events?target=&limit= | |
