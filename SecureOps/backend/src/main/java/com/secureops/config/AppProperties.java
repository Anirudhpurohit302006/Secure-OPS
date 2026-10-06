package com.secureops.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "secureops")
public record AppProperties(Ssh ssh, Auth auth, Collection collection, List<String> monitoredServices, Cors cors) {

    public record Ssh(String privateKeyPath, String privateKeyPassphrase, String password, String knownHostsPath,
                      int connectTimeoutMs, int commandTimeoutMs) {}

    public record Auth(String username, String passwordHash) {}

    public record Collection(boolean enabled, int intervalMinutes, int initialDelaySeconds) {
        public Collection {
            if (intervalMinutes < 5 || intervalMinutes > 45) {
                throw new IllegalArgumentException(
                        "secureops.collection.interval-minutes must be between 5 and 45 (normal: 30, maximum tolerance: 45)");
            }
        }
    }

    public record Cors(List<String> allowedOrigins) {}
}
