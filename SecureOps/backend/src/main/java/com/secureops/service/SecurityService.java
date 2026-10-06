package com.secureops.service;

import com.secureops.dto.Dtos.AuthEventDto;
import com.secureops.dto.Dtos.PortInfo;
import com.secureops.dto.Dtos.SecurityEventDto;
import com.secureops.dto.Dtos.SecurityFinding;
import com.secureops.dto.Dtos.SecurityResponse;
import com.secureops.exception.ApiException;
import com.secureops.model.Models.ListeningPort;
import com.secureops.model.Models.Machine;
import com.secureops.model.Models.SecurityState;
import com.secureops.repository.AuthRepository;
import com.secureops.repository.MachineRepository;
import com.secureops.repository.NetworkRepository;
import com.secureops.repository.SecurityRepository;
import com.secureops.util.TimeRange;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    private final SecurityRepository security;
    private final AuthRepository auth;
    private final NetworkRepository network;
    private final MachineRepository machines;
    private final SecurityAssessor assessor;
    private final TargetService targets;

    public SecurityService(SecurityRepository security, AuthRepository auth, NetworkRepository network,
                           MachineRepository machines, SecurityAssessor assessor, TargetService targets) {
        this.security = security;
        this.auth = auth;
        this.network = network;
        this.machines = machines;
        this.assessor = assessor;
        this.targets = targets;
    }

    public SecurityResponse security(int id) {
        Machine m = targets.require(id);
        Optional<SecurityState> state = security.latestState(id);
        List<ListeningPort> ports = network.latestPorts(id);
        if (state.isEmpty()) {
            return new SecurityResponse("UNKNOWN", null, null, null, null, false, null, false,
                    ports.stream().map(SecurityService::port).toList(), List.of(), null);
        }
        SecurityState s = state.get();
        List<SecurityFinding> findings = assessor.assess(id, m.machineName(), s, ports, s.timestamp());
        boolean listening = s.sshPort() != null && ports.stream().anyMatch(p -> p.protocol().equals("tcp") && p.port() == s.sshPort());
        return new SecurityResponse(SecurityAssessor.overallStatus(true, findings), s.firewallActive(), s.selinuxMode(),
                s.sshActive(), s.sshPort(), listening, s.sshConfig(), s.sshConfig() != null,
                ports.stream().map(SecurityService::port).toList(), findings, s.timestamp());
    }

    /** target = machine id or "all". */
    public List<AuthEventDto> authEvents(String target, String range, String type, int limit) {
        List<Integer> ids = targets.resolveIds(target);
        if (type != null && !type.equals("SSH_LOGIN_SUCCESS") && !type.equals("SSH_LOGIN_FAILED"))
            throw ApiException.badRequest("Unknown event type.");
        Map<Integer, String> names = names();
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minus(TimeRange.parse(range));
        return auth.list(ids, since, type, clamp(limit)).stream()
                .map(e -> new AuthEventDto(e.eventId(), e.machineId(), names.get(e.machineId()), e.username(),
                        e.sourceIp(), e.eventType(), e.timestamp())).toList();
    }

    public List<SecurityEventDto> securityEvents(String target, String range, int limit) {
        List<Integer> ids = targets.resolveIds(target);
        Map<Integer, String> names = names();
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minus(TimeRange.parse(range));
        return security.events(ids, since, clamp(limit)).stream()
                .map(e -> new SecurityEventDto(e.eventId(), e.machineId(), names.get(e.machineId()), e.eventType(),
                        e.severity(), e.description(), e.timestamp())).toList();
    }

    private Map<Integer, String> names() {
        Map<Integer, String> names = new HashMap<>();
        machines.findAll().forEach(m -> names.put(m.machineId(), m.machineName()));
        return names;
    }

    private static PortInfo port(ListeningPort p) { return new PortInfo(p.protocol(), p.port(), p.bindAddress(), p.state()); }

    private static int clamp(int limit) { return Math.max(1, Math.min(limit, 1000)); }
}
