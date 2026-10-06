package com.secureops.controller;

import com.secureops.dto.Dtos.AuthEventDto;
import com.secureops.dto.Dtos.MetricPoint;
import com.secureops.dto.Dtos.NetworkResponse;
import com.secureops.dto.Dtos.PortInfo;
import com.secureops.dto.Dtos.SecurityEventDto;
import com.secureops.dto.Dtos.SecurityResponse;
import com.secureops.dto.Dtos.ServiceHistoryPoint;
import com.secureops.dto.Dtos.ServiceState;
import com.secureops.dto.Dtos.SystemOverview;
import com.secureops.service.NetworkService;
import com.secureops.service.SecurityService;
import com.secureops.service.ServiceMonitor;
import com.secureops.service.SystemService;
import com.secureops.service.TargetService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/systems")
public class SystemController {
    private final SystemService systems;
    private final NetworkService network;
    private final ServiceMonitor serviceMonitor;
    private final SecurityService security;
    private final TargetService targets;

    public SystemController(SystemService systems, NetworkService network, ServiceMonitor serviceMonitor,
                            SecurityService security, TargetService targets) {
        this.systems = systems;
        this.network = network;
        this.serviceMonitor = serviceMonitor;
        this.security = security;
        this.targets = targets;
    }

    @GetMapping public List<SystemOverview> list() { return systems.overviews(targets.resolveIds("all")); }

    @GetMapping("/{id}") public SystemOverview get(@PathVariable int id) { return systems.overview(id); }

    @GetMapping("/{id}/metrics")
    public List<MetricPoint> metrics(@PathVariable int id, @RequestParam(defaultValue = "24h") String range) {
        return systems.metrics(id, range);
    }

    @GetMapping("/{id}/network") public NetworkResponse network(@PathVariable int id) { return network.network(id); }

    @GetMapping("/{id}/ports") public List<PortInfo> ports(@PathVariable int id) { return network.ports(id); }

    @GetMapping("/{id}/services") public List<ServiceState> services(@PathVariable int id) { return serviceMonitor.current(id); }

    @GetMapping("/{id}/services/history")
    public List<ServiceHistoryPoint> serviceHistory(@PathVariable int id, @RequestParam(defaultValue = "24h") String range) {
        return serviceMonitor.history(id, range);
    }

    @GetMapping("/{id}/security") public SecurityResponse security(@PathVariable int id) { return security.security(id); }

    /** {id} may be a machine id or "all". */
    @GetMapping("/{id}/auth-events")
    public List<AuthEventDto> authEvents(@PathVariable String id, @RequestParam(defaultValue = "24h") String range,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(defaultValue = "200") int limit) {
        return security.authEvents(id, range, type, limit);
    }

    @GetMapping("/{id}/security-events")
    public List<SecurityEventDto> securityEvents(@PathVariable String id, @RequestParam(defaultValue = "7d") String range,
                                                 @RequestParam(defaultValue = "200") int limit) {
        return security.securityEvents(id, range, limit);
    }
}
