package com.secureops.util;

import com.secureops.dto.Dtos.TargetRequest;
import com.secureops.exception.ApiException;
import java.util.regex.Pattern;

public final class Validation {
    private Validation() {}

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 ._-]{0,62}$");
    private static final Pattern USER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_.-]{0,31}$");
    private static final Pattern IPV4 =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    public static void validateTarget(TargetRequest r) {
        if (r == null) throw ApiException.badRequest("Request body is required.");
        if (r.machineName() == null || !NAME.matcher(r.machineName().trim()).matches())
            throw ApiException.badRequest("Machine name must be 1-63 characters: letters, digits, space, '.', '_' or '-'.");
        if (r.ipAddress() == null || !IPV4.matcher(r.ipAddress().trim()).matches())
            throw ApiException.badRequest("Target IP must be a valid IPv4 address.");
        if (r.sshUsername() == null || !USER.matcher(r.sshUsername().trim()).matches())
            throw ApiException.badRequest("SSH username is invalid.");
        if (r.sshPort() != null && (r.sshPort() < 1 || r.sshPort() > 65535))
            throw ApiException.badRequest("SSH port must be between 1 and 65535.");
    }
}
