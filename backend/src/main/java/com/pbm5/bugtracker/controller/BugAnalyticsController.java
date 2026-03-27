package com.pbm5.bugtracker.controller;

import com.pbm5.bugtracker.service.BugAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bugtracker/v1/projects/{projectSlug}/analytics")
@CrossOrigin(origins = "*")
public class BugAnalyticsController {

    @Autowired
    private BugAnalyticsService bugAnalyticsService;

    // Basic Statistics

    /**
     * Get comprehensive bug statistics for a project
     */
    @GetMapping("/statistics")
    public ResponseEntity<BugAnalyticsService.ProjectBugStatistics> getProjectStatistics(
            @PathVariable String projectSlug,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        BugAnalyticsService.ProjectBugStatistics statistics = bugAnalyticsService.getProjectStatistics(projectSlug,
                startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get team performance statistics
     */
    @GetMapping("/team-performance")
    public ResponseEntity<BugAnalyticsService.TeamPerformanceStatistics> getTeamPerformanceStatistics(
            @PathVariable String projectSlug,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        BugAnalyticsService.TeamPerformanceStatistics teamStats = bugAnalyticsService
                .getTeamPerformanceStatistics(projectSlug, startDate, endDate);
        return ResponseEntity.ok(teamStats);
    }

    // Trend Analysis

    // Reporting

    /**
     * Generate comprehensive project report
     */
    @GetMapping("/reports/comprehensive")
    public ResponseEntity<BugAnalyticsService.ProjectReport> generateProjectReport(
            @PathVariable String projectSlug,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        BugAnalyticsService.ProjectReport report = bugAnalyticsService.generateProjectReport(projectSlug, startDate,
                endDate);
        return ResponseEntity.ok(report);
    }
}