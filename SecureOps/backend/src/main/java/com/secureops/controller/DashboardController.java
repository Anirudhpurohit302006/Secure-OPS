package com.secureops.controller;

import com.secureops.dto.Dtos.DashboardSummary;
import com.secureops.dto.Dtos.EventItem;
import com.secureops.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(defaultValue = "all") String target) { return dashboard.summary(target); }

    @GetMapping("/events")
    public List<EventItem> events(@RequestParam(defaultValue = "all") String target, @RequestParam(defaultValue = "30") int limit) {
        return dashboard.events(target, limit);
    }
}
