package com.secureops.ssh;

import java.util.regex.Pattern;

/**
 * The complete allow-list of commands SecureOps may run on a target.
 * The constructor is private: nothing from the browser or the database can create a command.
 * The only parameterised command is {@link #serviceActive(String)}, whose argument is strictly validated.
 */
public final class ApprovedCommand {
    private static final Pattern SERVICE_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9@._-]{0,63}$");

    private final String command;

    private ApprovedCommand(String command) { this.command = command; }

    public String command() { return command; }

    @Override public String toString() { return command; }

    // ---- system ----
    public static final ApprovedCommand HOSTNAME = new ApprovedCommand("hostname");
    public static final ApprovedCommand OS_RELEASE = new ApprovedCommand("cat /etc/os-release");
    public static final ApprovedCommand KERNEL = new ApprovedCommand("uname -r");
    public static final ApprovedCommand MACHINE_ID = new ApprovedCommand("cat /etc/machine-id");
    public static final ApprovedCommand IP_ADDRESSES = new ApprovedCommand("hostname -I");
    public static final ApprovedCommand UPTIME = new ApprovedCommand("cat /proc/uptime");
    /** Two samples of the aggregate CPU line, one second apart, so utilisation reflects the present. */
    public static final ApprovedCommand CPU_SAMPLE = new ApprovedCommand("head -n 1 /proc/stat; sleep 1; head -n 1 /proc/stat");
    public static final ApprovedCommand MEMINFO = new ApprovedCommand("cat /proc/meminfo");
    public static final ApprovedCommand DISK = new ApprovedCommand("df -B1 -P");
    public static final ApprovedCommand DATE_OFFSET = new ApprovedCommand("date +%z");

    // ---- network ----
    public static final ApprovedCommand IP_BRIEF = new ApprovedCommand("ip -brief address");
    public static final ApprovedCommand NET_DEV = new ApprovedCommand("cat /proc/net/dev");
    public static final ApprovedCommand LISTENING_PORTS = new ApprovedCommand("ss -tuln");

    // ---- services / security ----
    public static final ApprovedCommand FAILED_UNITS = new ApprovedCommand("systemctl --failed --no-legend --plain");
    public static final ApprovedCommand SELINUX = new ApprovedCommand("getenforce");
    /** Drop-in files first (RHEL includes them at the top of sshd_config); falls back to a narrowly-scoped sudo rule. */
    public static final ApprovedCommand SSHD_CONFIG = new ApprovedCommand(
            "cat /etc/ssh/sshd_config.d/*.conf /etc/ssh/sshd_config 2>/dev/null"
                    + " || { cat /etc/ssh/sshd_config.d/*.conf 2>/dev/null; sudo -n /usr/bin/cat /etc/ssh/sshd_config 2>/dev/null; }");
    /** /var/log/secure is root-only on RHEL; falls back to a narrowly-scoped sudo rule. */
    public static final ApprovedCommand AUTH_LOG = new ApprovedCommand(
            "tail -n 5000 /var/log/secure 2>/dev/null || sudo -n /usr/bin/tail -n 5000 /var/log/secure 2>/dev/null");

    public static ApprovedCommand serviceActive(String serviceName) {
        if (serviceName == null || !SERVICE_NAME.matcher(serviceName).matches()) {
            throw new IllegalArgumentException("Invalid service name");
        }
        return new ApprovedCommand("systemctl is-active " + serviceName);
    }
}
