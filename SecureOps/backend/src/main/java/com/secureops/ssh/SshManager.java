package com.secureops.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.secureops.config.AppProperties;
import com.secureops.model.Models.Machine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Single place where SSH is used. Connections are temporary: open, run approved commands, close. */
@Component
public class SshManager {
    private static final Logger log = LoggerFactory.getLogger(SshManager.class);
    private static final int MAX_OUTPUT_BYTES = 2 * 1024 * 1024;

    private final AppProperties.Ssh cfg;
    private boolean warnedHostKeys;

    public SshManager(AppProperties props) {
        this.cfg = props.ssh();
    }

    public RemoteSession connect(Machine m) throws SshException {
        try {
            JSch jsch = new JSch();
            if (notBlank(cfg.privateKeyPath())) {
                if (!Files.isReadable(Path.of(cfg.privateKeyPath()))) {
                    throw new SshException("SSH private key file is not readable. Check SSH_PRIVATE_KEY_PATH.");
                }
                if (notBlank(cfg.privateKeyPassphrase())) jsch.addIdentity(cfg.privateKeyPath(), cfg.privateKeyPassphrase());
                else jsch.addIdentity(cfg.privateKeyPath());
            } else if (!notBlank(cfg.password())) {
                throw new SshException("No SSH credentials configured. Set SSH_PRIVATE_KEY_PATH (preferred) in config/secureops.env.");
            }

            boolean strict = notBlank(cfg.knownHostsPath()) && Files.isReadable(Path.of(cfg.knownHostsPath()));
            if (strict) {
                jsch.setKnownHosts(cfg.knownHostsPath());
            } else if (!warnedHostKeys) {
                warnedHostKeys = true;
                log.warn("SSH_KNOWN_HOSTS_PATH not set/readable: host keys are NOT verified. Configure it for real use.");
            }

            Session session = jsch.getSession(m.sshUsername(), m.ipAddress(), m.sshPort());
            if (notBlank(cfg.password())) session.setPassword(cfg.password());
            session.setConfig("StrictHostKeyChecking", strict ? "yes" : "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect(cfg.connectTimeoutMs());
            return new JschSession(session, cfg.commandTimeoutMs());
        } catch (JSchException e) {
            throw new SshException(friendly(e), e);
        }
    }

    static String friendly(JSchException e) {
        String msg = String.valueOf(e.getMessage()).toLowerCase();
        if (msg.contains("auth fail") || msg.contains("auth cancel")) return "SSH authentication failed (check username and key).";
        if (msg.contains("timeout") || msg.contains("timed out")) return "Connection timed out (is the VM powered on and reachable?).";
        if (msg.contains("refused")) return "Connection refused (is sshd running on the target?).";
        if (msg.contains("unknownhost") || msg.contains("unreachable") || msg.contains("no route")) return "Host unreachable.";
        if (msg.contains("hostkey") || msg.contains("reject")) return "Host key verification failed (known_hosts mismatch).";
        return "SSH connection failed.";
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static final class JschSession implements RemoteSession {
        private final Session session;
        private final int commandTimeoutMs;

        JschSession(Session session, int commandTimeoutMs) {
            this.session = session;
            this.commandTimeoutMs = commandTimeoutMs;
        }

        @Override
        public CommandResult run(ApprovedCommand command) throws SshException {
            ChannelExec channel = null;
            try {
                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command.command());
                channel.setInputStream(null);
                InputStream in = channel.getInputStream();
                channel.connect(commandTimeoutMs);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                long deadline = System.currentTimeMillis() + commandTimeoutMs;
                while (true) {
                    while (in.available() > 0) {
                        int n = in.read(buf, 0, buf.length);
                        if (n < 0) break;
                        if (out.size() < MAX_OUTPUT_BYTES) out.write(buf, 0, n);
                    }
                    if (channel.isClosed()) {
                        if (in.available() > 0) continue;
                        break;
                    }
                    if (System.currentTimeMillis() > deadline) throw new SshException("Remote command timed out.");
                    Thread.sleep(40);
                }
                return new CommandResult(channel.getExitStatus(), out.toString(StandardCharsets.UTF_8));
            } catch (JSchException | IOException e) {
                throw new SshException("Failed to run a remote command.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SshException("Interrupted while running a remote command.", e);
            } finally {
                if (channel != null) channel.disconnect();
            }
        }

        @Override
        public void close() { session.disconnect(); }
    }
}
