package com.secureops.service;

import com.secureops.dto.Dtos.ActionResult;
import com.secureops.dto.Dtos.TargetDto;
import com.secureops.dto.Dtos.TargetRequest;
import com.secureops.exception.ApiException;
import com.secureops.model.Models.Machine;
import com.secureops.repository.MachineRepository;
import com.secureops.ssh.ApprovedCommand;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import com.secureops.ssh.SshManager;
import com.secureops.util.Validation;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TargetService {
    private final MachineRepository machines;
    private final SshManager ssh;
    private final CollectionService collection;

    public TargetService(MachineRepository machines, SshManager ssh, CollectionService collection) {
        this.machines = machines;
        this.ssh = ssh;
        this.collection = collection;
    }

    public List<TargetDto> list() { return machines.findAll().stream().map(TargetDto::from).toList(); }

    public TargetDto get(int id) { return TargetDto.from(require(id)); }

    public Machine require(int id) {
        return machines.findById(id).orElseThrow(() -> ApiException.notFound("Target " + id + " was not found."));
    }

    public TargetDto create(TargetRequest r) {
        Validation.validateTarget(r);
        String name = r.machineName().trim();
        if (machines.nameExists(name, -1)) throw ApiException.badRequest("A target with this name already exists.");
        int id = machines.insert(name, r.ipAddress().trim(), r.sshUsername().trim(),
                r.sshPort() == null ? 22 : r.sshPort(), r.enabled() == null || r.enabled());
        return get(id);
    }

    public TargetDto update(int id, TargetRequest r) {
        Machine current = require(id);
        Validation.validateTarget(r);
        String name = r.machineName().trim();
        if (machines.nameExists(name, id)) throw ApiException.badRequest("A target with this name already exists.");
        machines.update(id, name, r.ipAddress().trim(), r.sshUsername().trim(),
                r.sshPort() == null ? current.sshPort() : r.sshPort(),
                r.enabled() == null ? current.enabled() : r.enabled());
        return get(id);
    }

    /** Deleting a target removes its history (foreign-key cascade). The UI asks for confirmation first. */
    public void delete(int id) {
        require(id);
        machines.delete(id);
    }

    public ActionResult test(int id) {
        Machine m = require(id);
        long start = System.currentTimeMillis();
        try (RemoteSession s = ssh.connect(m)) {
            String host = s.run(ApprovedCommand.HOSTNAME).trimmed();
            machines.updateStatus(id, "ONLINE", now());
            return new ActionResult(true, "Connected to " + (host.isBlank() ? m.ipAddress() : host) + ".", System.currentTimeMillis() - start);
        } catch (SshException e) {
            if (m.status().equals("ONLINE") || m.status().equals("UNKNOWN")) machines.updateStatus(id, "OFFLINE", null);
            return new ActionResult(false, e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    public ActionResult collectNow(int id) {
        Machine m = require(id);
        if (!m.enabled()) throw ApiException.badRequest("Target is disabled. Enable it before collecting.");
        return collection.collect(m);
    }

    /** "all" (or empty) -> every target; otherwise a single existing target id. */
    public List<Integer> resolveIds(String target) {
        if (target == null || target.isBlank() || target.equalsIgnoreCase("all")) {
            return machines.findAll().stream().map(Machine::machineId).toList();
        }
        try {
            int id = Integer.parseInt(target);
            require(id);
            return List.of(id);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid target '" + target + "'.");
        }
    }

    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS); }
}
