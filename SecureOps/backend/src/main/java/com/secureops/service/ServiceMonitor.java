package com.secureops.service;

import com.secureops.dto.Dtos.ServiceHistoryPoint;
import com.secureops.dto.Dtos.ServiceState;
import com.secureops.repository.ServiceRepository;
import com.secureops.util.TimeRange;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServiceMonitor {
    private final ServiceRepository services;
    private final TargetService targets;

    public ServiceMonitor(ServiceRepository services, TargetService targets) {
        this.services = services;
        this.targets = targets;
    }

    public List<ServiceState> current(int id) {
        targets.require(id);
        return services.latest(id).stream().map(s -> new ServiceState(s.serviceName(), s.status())).toList();
    }

    public List<ServiceHistoryPoint> history(int id, String range) {
        targets.require(id);
        LocalDateTime since = LocalDateTime.now(ZoneOffset.UTC).minus(TimeRange.parse(range));
        return services.series(List.of(id), since).stream()
                .map(s -> new ServiceHistoryPoint(s.timestamp(), s.serviceName(), s.status())).toList();
    }
}
