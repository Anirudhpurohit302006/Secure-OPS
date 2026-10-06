package com.secureops.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SystemCollectorTest {

    @Test
    void cpuUsageIsComputedFromTwoSamples() {
        String sample = "cpu  100 0 100 800 0 0 0 0 0 0\ncpu  150 0 150 900 0 0 0 0 0 0\n";
        assertThat(SystemCollector.parseCpuUsage(sample)).isEqualTo(50.0);   // dTotal=200, dIdle=100
    }

    @Test
    void cpuUsageTreatsIowaitAsIdle() {
        String sample = "cpu  100 0 100 700 100 0 0 0 0 0\ncpu  100 0 100 750 150 0 0 0 0 0\n";
        assertThat(SystemCollector.parseCpuUsage(sample)).isEqualTo(0.0);
    }

    @Test
    void cpuUsageWithNoElapsedTimeIsZero() {
        String same = "cpu  1 1 1 1 1 1 1 1 0 0\ncpu  1 1 1 1 1 1 1 1 0 0\n";
        assertThat(SystemCollector.parseCpuUsage(same)).isZero();
    }

    @Test
    void cpuRequiresTwoSamples() {
        assertThatThrownBy(() -> SystemCollector.parseCpuUsage("cpu  1 1 1 1 1 1 1 1 0 0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void memoryUsageUsesMemAvailable() {
        String meminfo = "MemTotal:        1000 kB\nMemFree:          100 kB\nMemAvailable:     250 kB\nBuffers: 5 kB\n";
        assertThat(SystemCollector.parseMemoryUsage(meminfo)).isEqualTo(75.0);
    }

    @Test
    void memoryUsageFallsBackWithoutMemAvailable() {
        String meminfo = "MemTotal: 1000 kB\nMemFree: 100 kB\nBuffers: 100 kB\nCached: 300 kB\n";
        assertThat(SystemCollector.parseMemoryUsage(meminfo)).isEqualTo(50.0);
    }

    @Test
    void diskUsageReadsRootFilesystem() {
        String df = "Filesystem 1-blocks Used Available Capacity Mounted on\n"
                + "/dev/mapper/rhel-root 100 30 70 30% /\n/dev/sda1 100 10 90 10% /boot\n";
        assertThat(SystemCollector.parseDiskUsage(df)).isEqualTo(30.0);
    }

    @Test
    void diskUsageIsNullWithoutRoot() {
        assertThat(SystemCollector.parseDiskUsage("Filesystem 1-blocks Used Available Capacity Mounted on\n")).isNull();
    }

    @Test
    void uptimeAndOsRelease() {
        assertThat(SystemCollector.parseUptimeSeconds("12345.67 999.0\n")).isEqualTo(12345L);
        var os = SystemCollector.parseOsRelease("NAME=\"Red Hat Enterprise Linux\"\nVERSION_ID=\"9.4\"\n# c\n");
        assertThat(os).containsEntry("NAME", "Red Hat Enterprise Linux").containsEntry("VERSION_ID", "9.4");
    }
}
