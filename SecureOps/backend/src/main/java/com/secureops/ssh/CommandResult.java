package com.secureops.ssh;

public record CommandResult(int exitCode, String stdout) {
    public String trimmed() { return stdout == null ? "" : stdout.trim(); }
}
