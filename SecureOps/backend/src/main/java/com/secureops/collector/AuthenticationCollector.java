package com.secureops.collector;

import com.secureops.model.Models.AuthEvent;
import com.secureops.ssh.ApprovedCommand;
import com.secureops.ssh.CommandResult;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationCollector {

    public static final String SUCCESS = "SSH_LOGIN_SUCCESS";
    public static final String FAILED = "SSH_LOGIN_FAILED";

    public record Result(List<AuthEvent> events, boolean logReadable) {}

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final Pattern TRADITIONAL = Pattern.compile(
            "^([A-Z][a-z]{2})\\s+(\\d{1,2})\\s+(\\d{2}):(\\d{2}):(\\d{2})\\s+\\S+\\s+sshd(?:-session)?\\[\\d+\\]:\\s+(.*)$");
    private static final Pattern ISO = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})\\s+\\S+\\s+sshd(?:-session)?\\[\\d+\\]:\\s+(.*)$");
    private static final Pattern ACCEPTED = Pattern.compile("^Accepted (\\S+) for (\\S+) from (\\S+) port \\d+.*");
    private static final Pattern FAILED_P = Pattern.compile("^Failed (\\S+) for (?:invalid user )?(\\S+) from (\\S+) port \\d+.*");

    public Result collect(RemoteSession s, int machineId, LocalDateTime nowUtc) throws SshException {
        ZoneOffset offset = parseOffset(s.run(ApprovedCommand.DATE_OFFSET).trimmed());
        CommandResult log = s.run(ApprovedCommand.AUTH_LOG);
        boolean readable = log.exitCode() == 0 || !log.stdout().isBlank();
        return new Result(parseLog(log.stdout(), machineId, nowUtc, offset), readable);
    }

    /** `date +%z` -> "+0530". Falls back to UTC. */
    public static ZoneOffset parseOffset(String s) {
        try {
            if (s.matches("[+-]\\d{4}")) return ZoneOffset.of(s.substring(0, 3) + ":" + s.substring(3));
        } catch (Exception ignored) { }
        return ZoneOffset.UTC;
    }

    public static List<AuthEvent> parseLog(String text, int machineId, LocalDateTime nowUtc, ZoneOffset remoteOffset) {
        List<AuthEvent> out = new ArrayList<>();
        for (String line : text.split("\\R")) {
            LocalDateTime ts;
            String msg;
            Matcher t = TRADITIONAL.matcher(line);
            Matcher i = ISO.matcher(line);
            if (t.matches()) {
                ts = traditionalToUtc(t, nowUtc, remoteOffset);
                msg = t.group(6);
            } else if (i.matches()) {
                ZoneOffset z = i.group(2).equals("Z") ? ZoneOffset.UTC : ZoneOffset.of(i.group(2));
                ts = LocalDateTime.parse(i.group(1)).atOffset(z).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
                msg = i.group(3);
            } else {
                continue;
            }
            if (ts == null) continue;
            Matcher a = ACCEPTED.matcher(msg);
            Matcher f = FAILED_P.matcher(msg);
            if (a.matches()) out.add(new AuthEvent(0, machineId, a.group(2), a.group(3), SUCCESS, ts));
            else if (f.matches()) out.add(new AuthEvent(0, machineId, f.group(2), f.group(3), FAILED, ts));
        }
        return out;
    }

    private static LocalDateTime traditionalToUtc(Matcher m, LocalDateTime nowUtc, ZoneOffset offset) {
        int month = -1;
        for (int k = 0; k < 12; k++) if (MONTHS[k].equals(m.group(1))) month = k + 1;
        if (month < 0) return null;
        int year = nowUtc.getYear();
        try {
            LocalDateTime utc = toUtc(year, month, m, offset);
            if (utc.isAfter(nowUtc.plusDays(1))) utc = toUtc(year - 1, month, m, offset);   // log line from last year
            return utc;
        } catch (Exception e) {
            return null;   // e.g. Feb 29 in a non-leap year
        }
    }

    private static LocalDateTime toUtc(int year, int month, Matcher m, ZoneOffset offset) {
        LocalDateTime local = LocalDateTime.of(year, month, Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)));
        return OffsetDateTime.of(local, offset).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
