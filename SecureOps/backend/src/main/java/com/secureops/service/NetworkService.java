package com.secureops.service;

import com.secureops.dto.Dtos.InterfaceInfo;
import com.secureops.dto.Dtos.NetworkResponse;
import com.secureops.dto.Dtos.PortInfo;
import com.secureops.model.Models.NetworkMetric;
import com.secureops.repository.NetworkRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NetworkService {
    private final NetworkRepository network;
    private final TargetService targets;

    public NetworkService(NetworkRepository network, TargetService targets) {
        this.network = network;
        this.targets = targets;
    }

    public NetworkResponse network(int id) {
        targets.require(id);
        List<NetworkMetric> latest = network.latestMetrics(id);
        return new NetworkResponse(latest.isEmpty() ? null : latest.get(0).timestamp(),
                latest.stream().map(n -> new InterfaceInfo(n.interfaceName(), n.state(), n.ipAddress(), n.rxBytes(), n.txBytes())).toList());
    }

    public List<PortInfo> ports(int id) {
        targets.require(id);
        return network.latestPorts(id).stream()
                .map(p -> new PortInfo(p.protocol(), p.port(), p.bindAddress(), p.state())).toList();
    }
}
