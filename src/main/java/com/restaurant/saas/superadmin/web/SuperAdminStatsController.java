package com.restaurant.saas.superadmin.web;

import com.restaurant.saas.superadmin.service.SuperAdminStatsService;
import com.restaurant.saas.superadmin.service.SuperAdminStatsService.CustomersReportResponse;
import com.restaurant.saas.superadmin.service.SuperAdminStatsService.SalesReportResponse;
import com.restaurant.saas.superadmin.service.SuperAdminStatsService.StatsResponse;
import com.restaurant.saas.superadmin.service.SuperAdminStatsService.UsageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperAdminStatsController {

    private final SuperAdminStatsService statsService;

    public SuperAdminStatsController(SuperAdminStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return statsService.stats();
    }

    @GetMapping("/stats/revenue")
    public List<SuperAdminStatsService.MonthAmount> revenue(@RequestParam(defaultValue = "12") int months) {
        return statsService.revenue(months);
    }

    @GetMapping("/reports/sales")
    public SalesReportResponse sales(@RequestParam(defaultValue = "30") int days) {
        return statsService.sales(days);
    }

    @GetMapping("/reports/customers")
    public CustomersReportResponse customers(@RequestParam(defaultValue = "30") int days) {
        return statsService.customers(days);
    }

    @GetMapping("/tenants/{id}/usage")
    public UsageResponse usage(@PathVariable Long id) {
        return statsService.usage(id);
    }
}
