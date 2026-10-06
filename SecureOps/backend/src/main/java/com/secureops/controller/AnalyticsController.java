package com.secureops.controller;

import com.secureops.dto.Dtos.AnalyticsPoint;
import com.secureops.dto.Dtos.AuthAnalytics;
import com.secureops.dto.Dtos.SecurityAnalytics;
import com.secureops.dto.Dtos.ServiceAnalytics;
import com.secureops.service.AnalyticsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** target = machine id or "all"; range = 6h | 24h | 7d | 30d. */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) { this.analytics = analytics; }

    @GetMapping("/cpu")
    public List<AnalyticsPoint> cpu(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "24h") String range) {
        return analytics.cpu(target, range);
    }

    @GetMapping("/memory")
    public List<AnalyticsPoint> memory(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "24h") String range) {
        return analytics.memory(target, range);
    }

    @GetMapping("/disk")
    public List<AnalyticsPoint> disk(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "24h") String range) {
        return analytics.disk(target, range);
    }

    @GetMapping("/network")
    public List<AnalyticsPoint> network(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "24h") String range) {
        return analytics.network(target, range);
    }

    @GetMapping("/services")
    public ServiceAnalytics services(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "24h") String range) {
        return analytics.services(target, range);
    }

    @GetMapping("/authentication")
    public AuthAnalytics authentication(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "24h") String range) {
        return analytics.authentication(target, range);
    }

    @GetMapping("/security")
    public SecurityAnalytics security(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "24h") String range) {
        return analytics.security(target, range);
    }
}
