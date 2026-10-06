# RHEL commands used by the collectors

All are defined in `backend/.../ssh/ApprovedCommand.java`. Nothing else can be executed: the constructor is private, and the only
parameterised command (`systemctl is-active <unit>`) validates the unit name against `^[A-Za-z0-9][A-Za-z0-9@._-]{0,63}$`.
All commands are read-only.

| Data | Command | Parsed by |
|---|---|---|
| Hostname | `hostname` | `SystemCollector` |
| OS / version | `cat /etc/os-release` (NAME, VERSION_ID) | `SystemCollector.parseOsRelease` |
| Kernel | `uname -r` | |
| Machine ID | `cat /etc/machine-id` | |
| IP | `hostname -I` (first address) | |
| Uptime | `cat /proc/uptime` | `parseUptimeSeconds` |
| CPU | `head -n 1 /proc/stat; sleep 1; head -n 1 /proc/stat` | `parseCpuUsage`: `(1 - dIdle/dTotal) * 100`, idle = idle+iowait |
| Memory | `cat /proc/meminfo` | `parseMemoryUsage`: `(MemTotal - MemAvailable) / MemTotal` |
| Disk | `df -B1 -P` (root filesystem `/`) | `parseDiskUsage`: `used / (used + available)` |
| Interfaces | `ip -brief address` | `NetworkCollector.parseIpBrief` |
| Traffic | `cat /proc/net/dev` (rx/tx bytes; `lo` skipped) | `parseNetDev` |
| Listening ports | `ss -tuln` | `parsePorts` |
| Services | `systemctl is-active sshd` / `firewalld` / `httpd` (+ extras from `MONITORED_SERVICES`) | `ServiceCollector.mapState` |
| Failed units | `systemctl --failed --no-legend --plain` | `parseFailedUnits` |
| SELinux | `getenforce` | `SecurityCollector.parseSelinux` |
| sshd settings | `cat /etc/ssh/sshd_config.d/*.conf /etc/ssh/sshd_config` (falls back to `sudo -n /usr/bin/cat /etc/ssh/sshd_config`) | `parseSshdConfig` (Port, PermitRootLogin, PasswordAuthentication, PermitEmptyPasswords, PubkeyAuthentication, X11Forwarding, MaxAuthTries; first occurrence wins) |
| Auth log | `tail -n 5000 /var/log/secure` (falls back to `sudo -n /usr/bin/tail -n 5000 /var/log/secure`) | `AuthenticationCollector.parseLog` (`Accepted ...` / `Failed ...` lines from sshd; traditional and ISO timestamps) |
| Time zone | `date +%z` | converts log times to UTC |

## Privileged files
On RHEL, `/var/log/secure` and `/etc/ssh/sshd_config` are root-only. `scripts/rhel-setup.sh <user>` installs this sudoers rule
(exact arguments, nothing else):
```
<user> ALL=(root) NOPASSWD: /usr/bin/tail -n 5000 /var/log/secure, /usr/bin/cat /etc/ssh/sshd_config
```
Without it SecureOps still works: login history stays empty, and the SSH-hardening checks show an informational
"could not be read" finding.

## Events
- **STARTUP**: first collection of a machine (boot time = now - uptime), or uptime reset after the machine was offline.
- **REBOOT**: uptime reset while the machine was online.
- **SHUTDOWN**: machine changed ONLINE -> unreachable (recorded as "possible shutdown").
- **SERVICE_FAILURE / SERVICE_STOP / SERVICE_START**: monitored service state transitions and `systemctl --failed` units.
- **Auth events**: successful/failed SSH logins. 5+ new failures from one IP in one cycle => `SSH_BRUTE_FORCE` (MEDIUM, HIGH at 10+). Successful root login => `ROOT_LOGIN`.
