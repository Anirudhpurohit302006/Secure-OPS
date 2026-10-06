#!/usr/bin/env bash
# Run ON EACH RHEL VM as root. Makes the monitoring account able to read only what SecureOps needs.
# Usage: sudo ./rhel-setup.sh <ssh-username>   (e.g. your existing user, or a dedicated "secureops" user)
set -euo pipefail
USER_NAME="${1:?usage: rhel-setup.sh <ssh-username>}"
id "$USER_NAME" >/dev/null 2>&1 || { echo "User $USER_NAME does not exist"; exit 1; }

# Exactly two privileged, read-only commands, byte-for-byte what SecureOps runs (see documentation/COLLECTORS.md).
cat > /etc/sudoers.d/secureops <<RULE
$USER_NAME ALL=(root) NOPASSWD: /usr/bin/tail -n 5000 /var/log/secure, /usr/bin/cat /etc/ssh/sshd_config
RULE
chmod 440 /etc/sudoers.d/secureops
visudo -cf /etc/sudoers.d/secureops
echo "OK: $USER_NAME may read /var/log/secure (last 5000 lines) and /etc/ssh/sshd_config via sudo, nothing else."
