package io.sandboxdev.gitmcp.debug.management;

import io.sandboxdev.gitmcp.debug.model.ErrorAnalysisReport;
import io.sandboxdev.gitmcp.debug.model.HealthReport;
import io.sandboxdev.gitmcp.debug.model.PerformanceReport;
import io.sandboxdev.gitmcp.debug.model.ReportSchedule;
import io.sandboxdev.gitmcp.debug.model.TimeRange;

/**
 * Interface for generating various types of debug reports.
 * Handles performance, error analysis, and health reports.
 */
public interface ReportGenerator {
    
    /**
     * Generate a performance report for the specified time range.
     * 
     * @param timeRange the time range for the report
     * @return the generated performance report
     */
    PerformanceReport generatePerformanceReport(TimeRange timeRange);
    
    /**
     * Generate an error analysis report for the specified time range.
     * 
     * @param timeRange the time range for the report
     * @return the generated error analysis report
     */
    ErrorAnalysisReport generateErrorReport(TimeRange timeRange);
    
    /**
     * Generate a health report for the current system state.
     * 
     * @return the generated health report
     */
    HealthReport generateHealthReport();
    
    /**
     * Schedule automatic report generation.
     * 
     * @param schedule the schedule for automatic report generation
     */
    void scheduleReports(ReportSchedule schedule);
}