package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.response.DashboardResponse;
import com.skr.erp.dto.response.DailyReportRow;
import com.skr.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<CommonResponse<DashboardResponse>> getOverview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        DashboardResponse data = dashboardService.getDashboard(fromDate, toDate);
        return ResponseEntity.ok(
                CommonResponse.<DashboardResponse>builder()
                        .success(true)
                        .message("Dashboard overview fetched successfully")
                        .data(data)
                        .build());
    }

    @GetMapping("/daily")
    public ResponseEntity<CommonResponse<List<DailyReportRow>>> getDailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        List<DailyReportRow> rows = dashboardService.getDailyReport(fromDate, toDate);
        return ResponseEntity.ok(
                CommonResponse.<List<DailyReportRow>>builder()
                        .success(true)
                        .message("Daily report fetched successfully")
                        .data(rows)
                        .build());
    }
}
