package io.sandboxdev.gitmcp.debug.management;

import io.sandboxdev.gitmcp.debug.model.ExportFormat;
import io.sandboxdev.gitmcp.debug.model.ImportFormat;
import io.sandboxdev.gitmcp.debug.model.TimeRange;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for exporting and importing debug data.
 * Handles data export in various formats and import validation.
 */
public interface DataExportService {
    
    /**
     * Export protocol traces to the specified output stream.
     * 
     * @param format the export format to use
     * @param output the output stream to write to
     */
    void exportProtocolTraces(ExportFormat format, OutputStream output);
    
    /**
     * Export performance metrics for the specified time range.
     * 
     * @param timeRange the time range for metrics export
     * @param format the export format to use
     * @param output the output stream to write to
     */
    void exportPerformanceMetrics(TimeRange timeRange, ExportFormat format, OutputStream output);
    
    /**
     * Configure whether to sanitize sensitive data during export.
     * 
     * @param sanitizeSensitiveData true to sanitize sensitive data, false otherwise
     */
    void sanitizeExportData(boolean sanitizeSensitiveData);
    
    /**
     * Import debugging data from the specified input stream.
     * 
     * @param input the input stream to read from
     * @param format the format of the imported data
     */
    void importDebuggingData(InputStream input, ImportFormat format);
}