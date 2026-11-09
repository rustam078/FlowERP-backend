package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateEmployeeRequest;
import com.skr.erp.dto.request.MarkAttendanceRequest;
import com.skr.erp.dto.request.BulkAdvanceRequest;
import com.skr.erp.dto.request.MarkAttendanceRangeRequest;
import com.skr.erp.dto.request.PayrollQuery;
import com.skr.erp.dto.request.RecordLedgerRequest;
import com.skr.erp.dto.request.UpdateEmployeeRequest;
import com.skr.erp.dto.request.UpdateEmployeeStatusRequest;
import com.skr.erp.dto.response.AttendanceResponse;
import com.skr.erp.dto.response.EmployeeCalendarResponse;
import com.skr.erp.dto.response.EmployeeDetailsResponse;
import com.skr.erp.dto.response.EmployeeResponse;
import com.skr.erp.dto.response.EmployeeStatsResponse;
import com.skr.erp.dto.response.LedgerSummaryResponse;
import com.skr.erp.dto.response.PayrollResponse;
import com.skr.erp.report.ReportDownload;
import com.skr.erp.service.EmployeeLedgerService;
import com.skr.erp.service.EmployeeService;
import com.skr.erp.service.PayslipService;
import com.skr.erp.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", exposedHeaders = "Content-Disposition")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ReportService reportService;
    private final EmployeeLedgerService employeeLedgerService;
    private final PayslipService payslipService;

    @PostMapping
    public CommonResponse<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        return CommonResponse
                .<EmployeeResponse>builder()
                .success(true)
                .message("Employee created successfully")
                .data(employeeService.create(request))
                .build();
    }

    @GetMapping
    public CommonResponse<List<EmployeeResponse>> getAll() {
        return CommonResponse.<List<EmployeeResponse>>builder()
                .success(true).message("Employees fetched successfully")
                .data(employeeService.getAll()).build();
    }

    @GetMapping("/{id}")
    public CommonResponse<EmployeeResponse> getById(@PathVariable UUID id) {
        return CommonResponse.<EmployeeResponse>builder().success(true)
                .message("Employee fetched successfully")
                .data(employeeService.getById(id)).build();
    }

    @PutMapping("/{id}")
    public CommonResponse<EmployeeResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest request) {

        return CommonResponse.<EmployeeResponse>builder().success(true)
                .message("Employee updated successfully")
                .data(employeeService.update(id, request)).build();
    }

    @PatchMapping("/{id}/status")
    public CommonResponse<String> updateStatus(@PathVariable UUID id, @RequestBody UpdateEmployeeStatusRequest request) {
        employeeService.updateStatus(id, request.getActive());
        return CommonResponse.<String>builder().success(true)
                .message("Employee status updated successfully")
                .data("Success").build();
    }

    @GetMapping("/{id}/details")
    public CommonResponse<EmployeeDetailsResponse> getDetails(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return CommonResponse.<EmployeeDetailsResponse>builder().success(true)
                .message("Employee details fetched successfully")
                .data(employeeService.getDetails(id, fromDate, toDate)).build();
    }

    @GetMapping("/stats")
    public CommonResponse<EmployeeStatsResponse> getStats() {
        return CommonResponse.<EmployeeStatsResponse>builder().success(true)
                .message("Employee statistics fetched successfully")
                .data(employeeService.getStats()).build();

    }

    // ── Attendance (exception-based leave/absent marking) ──
    @GetMapping("/{id}/attendance")
    public CommonResponse<List<AttendanceResponse>> getAttendance(@PathVariable UUID id,
            @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return CommonResponse.<List<AttendanceResponse>>builder().success(true)
                .message("Attendance fetched successfully")
                .data(employeeService.getAttendance(id, fromDate, toDate)).build();
    }

    @PostMapping("/{id}/attendance")
    public CommonResponse<AttendanceResponse> markAttendance(@PathVariable UUID id, @Valid @RequestBody MarkAttendanceRequest request) {
        return CommonResponse.<AttendanceResponse>builder().success(true)
                .message("Attendance marked")
                .data(employeeService.markAttendance(id, request)).build();
    }

    @PostMapping("/{id}/attendance/range")
    public CommonResponse<List<AttendanceResponse>> markAttendanceRange(@PathVariable UUID id, @Valid @RequestBody MarkAttendanceRangeRequest request) {
        return CommonResponse.<List<AttendanceResponse>>builder().success(true)
                .message("Attendance marked")
                .data(employeeService.markAttendanceRange(id, request)).build();
    }

    @DeleteMapping("/{id}/attendance")
    public CommonResponse<String> clearAttendance(@PathVariable UUID id, @RequestParam LocalDate date) {
        employeeService.clearAttendance(id, date);
        return CommonResponse.<String>builder().success(true).message("Attendance cleared").data("Success").build();
    }

    @GetMapping("/{id}/report/pdf")
    public ResponseEntity<byte[]> reportPdf(@PathVariable UUID id,
                                            @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return ReportDownload.pdf(reportService.employeePdf(id, fromDate, toDate),
                "employee_" + reportService.fileToken(fromDate, toDate) + ".pdf");
    }

    @GetMapping("/{id}/report/excel")
    public ResponseEntity<byte[]> reportExcel(@PathVariable UUID id,
                                              @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return ReportDownload.excel(reportService.employeeExcel(id, fromDate, toDate),
                "employee_" + reportService.fileToken(fromDate, toDate) + ".xlsx");
    }

    // ── Payroll: monthly pay-slips + advances/settlements + net pending (paged/filtered) ──
    @GetMapping("/{id}/payroll")
    public CommonResponse<PayrollResponse> getPayroll(@PathVariable UUID id, PayrollQuery query) {
        return CommonResponse.<PayrollResponse>builder().success(true)
                .message("Payroll fetched successfully")
                .data(employeeLedgerService.getPayroll(id, query)).build();
    }

    // Pay-slip PDF: by month (monthly staff) or by settlement entry (piece-rate staff).
    @GetMapping("/{id}/payslip")
    public ResponseEntity<byte[]> payslip(@PathVariable UUID id,
            @RequestParam(required = false) LocalDate period,
            @RequestParam(required = false) UUID ledgerId) {
        byte[] pdf = ledgerId != null
                ? payslipService.settlementPayslip(id, ledgerId)
                : payslipService.monthlyPayslip(id, period);
        return ReportDownload.pdf(pdf, "payslip.pdf");
    }

    @PostMapping("/{id}/ledger")
    public CommonResponse<PayrollResponse> recordLedger(@PathVariable UUID id, @Valid @RequestBody RecordLedgerRequest request) {
        return CommonResponse.<PayrollResponse>builder().success(true)
                .message("Transaction recorded")
                .data(employeeLedgerService.recordEntry(id, request)).build();
    }

    @DeleteMapping("/{id}/ledger/{entryId}")
    public CommonResponse<PayrollResponse> deleteLedger(@PathVariable UUID id, @PathVariable UUID entryId) {
        return CommonResponse.<PayrollResponse>builder().success(true)
                .message("Transaction removed")
                .data(employeeLedgerService.deleteEntry(id, entryId)).build();
    }

    // Give an advance to several employees at once (bulk screen).
    @PostMapping("/advances/bulk")
    public CommonResponse<Integer> bulkAdvance(@Valid @RequestBody BulkAdvanceRequest request) {
        return CommonResponse.<Integer>builder().success(true)
                .message("Advances recorded")
                .data(employeeLedgerService.bulkAdvance(request)).build();
    }

    // Legacy ledger summary, still used by the current card until the new payroll UI ships.
    @GetMapping("/{id}/ledger")
    public CommonResponse<LedgerSummaryResponse> getLedger(@PathVariable UUID id) {
        return CommonResponse.<LedgerSummaryResponse>builder().success(true)
                .message("Ledger fetched successfully")
                .data(employeeLedgerService.getLedger(id)).build();
    }

    @GetMapping("/{employeeId}/calendar")
    public CommonResponse<List<EmployeeCalendarResponse>> getCalendarData(
            @PathVariable UUID employeeId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {

        return CommonResponse.<List<EmployeeCalendarResponse>>builder().success(true)
                .message("Calendar data fetched successfully")
                .data(employeeService.getCalendarData(employeeId, fromDate, toDate)).build();
    }

}