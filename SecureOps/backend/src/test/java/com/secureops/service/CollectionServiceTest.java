package com.secureops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.secureops.collector.AuthenticationCollector;
import com.secureops.collector.NetworkCollector;
import com.secureops.collector.SecurityCollector;
import com.secureops.collector.ServiceCollector;
import com.secureops.collector.SystemCollector;
import com.secureops.config.AppProperties;
import com.secureops.dto.Dtos.ActionResult;
import com.secureops.model.Models.Machine;
import com.secureops.model.Models.SystemEvent;
import com.secureops.model.Models.SystemMetric;
import com.secureops.repository.AuthRepository;
import com.secureops.repository.EventRepository;
import com.secureops.repository.MachineRepository;
import com.secureops.repository.MetricsRepository;
import com.secureops.repository.NetworkRepository;
import com.secureops.repository.SecurityRepository;
import com.secureops.repository.ServiceRepository;
import com.secureops.ssh.ApprovedCommand;
import com.secureops.ssh.CommandResult;
import com.secureops.ssh.RemoteSession;
import com.secureops.ssh.SshException;
import com.secureops.ssh.SshManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

/** Runs the whole collection pipeline against a fake SSH session (no RHEL required). */
class CollectionServiceTest {
    private SshManager ssh;
    private MachineRepository machines;
    private MetricsRepository metrics;
    private NetworkRepository network;
    private ServiceRepository services;
    private AuthRepository auth;
    private SecurityRepository security;
    private EventRepository events;
    private CollectionService service;

    @BeforeEach
    void setUp() {
        ssh = mock(SshManager.class);
        machines = mock(MachineRepository.class);
        metrics = mock(MetricsRepository.class);
        network = mock(NetworkRepository.class);
        services = mock(ServiceRepository.class);
        auth = mock(AuthRepository.class);
        security = mock(SecurityRepository.class);
        events = mock(EventRepository.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        doAnswer(inv -> {
            ((Consumer<?>) inv.getArgument(0)).getClass();
            @SuppressWarnings("unchecked") Consumer<Object> c = (Consumer<Object>) inv.getArgument(0);
            c.accept(null);
            return null;
        }).when(tx).executeWithoutResult(any());

        var props = new AppProperties(null, null, null, List.of("sshd", "firewalld", "httpd"), null);
        service = new CollectionService(ssh, new SystemCollector(), new NetworkCollector(), new ServiceCollector(props),
                new AuthenticationCollector(), new SecurityCollector(), new SecurityAssessor(), machines, metrics,
                network, services, auth, security, events, tx);
    }

    private static Machine machine(String status) {
        return new Machine(1, "RHEL-01", null, "192.168.159.129", "ani", 22, true, status, null, null, null, null, null);
    }

    private static RemoteSession fakeSession() {
        Map<String, String> out = Map.ofEntries(
                Map.entry(ApprovedCommand.HOSTNAME.command(), "rhel01\n"),
                Map.entry(ApprovedCommand.OS_RELEASE.command(), "NAME=\"Red Hat Enterprise Linux\"\nVERSION_ID=\"9.4\"\n"),
                Map.entry(ApprovedCommand.KERNEL.command(), "5.14.0\n"),
                Map.entry(ApprovedCommand.MACHINE_ID.command(), "abc123\n"),
                Map.entry(ApprovedCommand.IP_ADDRESSES.command(), "192.168.159.129 \n"),
                Map.entry(ApprovedCommand.UPTIME.command(), "3600.00 100.00\n"),
                Map.entry(ApprovedCommand.CPU_SAMPLE.command(), "cpu  100 0 100 800 0 0 0 0 0 0\ncpu  150 0 150 900 0 0 0 0 0 0\n"),
                Map.entry(ApprovedCommand.MEMINFO.command(), "MemTotal: 1000 kB\nMemAvailable: 500 kB\n"),
                Map.entry(ApprovedCommand.DISK.command(), "Filesystem 1-blocks Used Available Capacity Mounted on\n/dev/x 100 40 60 40% /\n"),
                Map.entry(ApprovedCommand.IP_BRIEF.command(), "ens160 UP 192.168.159.129/24\n"),
                Map.entry(ApprovedCommand.NET_DEV.command(), "face|x\n ens160: 100 1 0 0 0 0 0 0 200 1 0 0 0 0 0 0\n"),
                Map.entry(ApprovedCommand.LISTENING_PORTS.command(), "tcp LISTEN 0 128 0.0.0.0:22 0.0.0.0:*\n"),
                Map.entry(ApprovedCommand.FAILED_UNITS.command(), ""),
                Map.entry(ApprovedCommand.SELINUX.command(), "Enforcing\n"),
                Map.entry(ApprovedCommand.SSHD_CONFIG.command(), "PermitRootLogin no\nPasswordAuthentication no\n"),
                Map.entry(ApprovedCommand.DATE_OFFSET.command(), "+0000\n"),
                Map.entry(ApprovedCommand.AUTH_LOG.command(), ""),
                Map.entry("systemctl is-active sshd", "active\n"),
                Map.entry("systemctl is-active firewalld", "active\n"),
                Map.entry("systemctl is-active httpd", "inactive\n"));
        return new RemoteSession() {
            @Override public CommandResult run(ApprovedCommand c) { return new CommandResult(0, out.getOrDefault(c.command(), "")); }
            @Override public void close() { }
        };
    }

    @Test
    void successfulCollectionPersistsHistoryAndMarksOnline() throws Exception {
        when(ssh.connect(any())).thenReturn(fakeSession());
        when(metrics.latest(anyInt())).thenReturn(Optional.empty());
        when(services.latest(anyInt())).thenReturn(List.of());

        ActionResult r = service.collect(machine("UNKNOWN"));

        assertThat(r.success()).isTrue();
        ArgumentCaptor<SystemMetric> captor = ArgumentCaptor.forClass(SystemMetric.class);
        verify(metrics).insert(captor.capture());
        assertThat(captor.getValue().cpuUsage()).isEqualTo(50.0);
        assertThat(captor.getValue().memoryUsage()).isEqualTo(50.0);
        assertThat(captor.getValue().diskUsage()).isEqualTo(40.0);
        assertThat(captor.getValue().uptime()).isEqualTo(3600L);
        verify(network).insertMetrics(any());
        verify(network).insertPorts(any());
        verify(services).insertAll(any());
        verify(security).insertState(any());
        verify(machines).updateStatus(eq(1), eq("ONLINE"), any());
        verify(events).insert(any(SystemEvent.class));   // baseline STARTUP event
    }

    @Test
    void uptimeResetIsRecordedAsReboot() throws Exception {
        when(ssh.connect(any())).thenReturn(fakeSession());
        when(metrics.latest(anyInt())).thenReturn(Optional.of(new SystemMetric(1, 1, null, 1.0, 1.0, 1.0, 999_999L)));
        when(services.latest(anyInt())).thenReturn(List.of());

        service.collect(machine("ONLINE"));

        ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(events).insert(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("REBOOT");
    }

    @Test
    void offlineTargetKeepsHistoryAndLastSeen() throws Exception {
        when(ssh.connect(any())).thenThrow(new SshException("Connection timed out"));

        ActionResult r = service.collect(machine("ONLINE"));

        assertThat(r.success()).isFalse();
        verify(machines).updateStatus(1, "OFFLINE", null);        // null => last_seen untouched
        verify(metrics, never()).insert(any());
        ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(events).insert(captor.capture());                  // ONLINE -> OFFLINE transition
        assertThat(captor.getValue().eventType()).isEqualTo("SHUTDOWN");
    }

    @Test
    void alreadyOfflineTargetDoesNotRepeatTheEvent() throws Exception {
        when(ssh.connect(any())).thenThrow(new SshException("Connection timed out"));
        service.collect(machine("OFFLINE"));
        verify(events, never()).insert(any());
        verify(machines).updateStatus(eq(1), eq("OFFLINE"), isNull());
    }
}
