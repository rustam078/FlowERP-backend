package com.skr.erp.service;

import com.skr.erp.common.constants.LedgerEntryType;
import com.skr.erp.common.constants.PaymentType;
import com.skr.erp.dto.response.PaySlipResponse;
import com.skr.erp.entity.Employee;
import com.skr.erp.entity.EmployeeLedger;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.report.ReportSupport;
import com.skr.erp.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Builds a single employee pay-slip PDF: a monthly salary slip, or a piece-work payment slip. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayslipService {

    private static final String TEMPLATE = "PAYSLIP_TEMPLATE";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter SETTLED = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final EmployeeRepository employeeRepository;
    private final EmployeeLedgerService ledgerService;
    private final ReportSupport support;

    // Monthly salary slip for a given month (salary, leave deduction, net).
    public byte[] monthlyPayslip(UUID employeeId, LocalDate period) {
        Employee e = findEmployee(employeeId);
        PaySlipResponse slip = ledgerService.slipForMonth(employeeId, period);
        if ("INACTIVE".equals(slip.getMonthStatus())) {
            throw new BusinessException("No payslip for " + slip.getMonthLabel() + " — the employee was inactive all month");
        }
        StringBuilder rows = new StringBuilder();
        row(rows, "Basic Salary (" + slip.getMonthLabel() + ")", slip.getBaseSalary());
        if (slip.getLeaveDeduction() != null && slip.getLeaveDeduction().signum() > 0) {
            row(rows, "Less: Leave Deduction (" + days(slip.getLeaveDays()) + ")", slip.getLeaveDeduction().negate());
        }
        LocalDate settled = ledgerService.settlementDateForMonth(employeeId, period);
        Map<String, String> v = header(e, "SALARY SLIP", slip.getMonthLabel(),
                settled == null ? "Not settled" : settled.format(SETTLED));
        v.put("rows", rows.toString());
        v.put("netLabel", "Net Salary");
        v.put("netAmount", support.money(slip.getEarned()));
        v.put("paidVia", "Salary");
        v.put("status", monthlyStatus(slip));
        return support.renderPdf(TEMPLATE, v);
    }

    // Piece-work payment slip for one settlement entry (direct payment or advance adjustment).
    public byte[] settlementPayslip(UUID employeeId, UUID entryId) {
        Employee e = findEmployee(employeeId);
        EmployeeLedger entry = ledgerService.settlementEntry(employeeId, entryId);
        boolean fromAdvance = entry.getType() == LedgerEntryType.ADVANCE_ADJUSTMENT;
        String date = entry.getEntryDate() == null ? "-" : entry.getEntryDate().format(DAY);
        String settled = entry.getEntryDate() == null ? "-" : entry.getEntryDate().format(SETTLED);
        StringBuilder rows = new StringBuilder();
        row(rows, fromAdvance ? "Piece Work Payment (settled from advance)" : "Piece Work Payment", entry.getAmount());
        Map<String, String> v = header(e, "PAYMENT SLIP", date, settled);
        v.put("rows", rows.toString());
        v.put("netLabel", "Amount Paid");
        v.put("netAmount", support.money(entry.getAmount()));
        v.put("paidVia", fromAdvance ? "Advance Adjustment" : (entry.getMode() == null ? "Cash" : entry.getMode().name()));
        v.put("status", "Paid");
        return support.renderPdf(TEMPLATE, v);
    }

    private Map<String, String> header(Employee e, String title, String periodLabel, String settlementDate) {
        Map<String, String> v = new HashMap<>();
        v.put("title", title);
        v.put("periodLabel", periodLabel);
        v.put("settlementDate", settlementDate);
        v.put("employeeName", support.esc(e.getFullName()));
        v.put("employeeCode", support.esc(e.getEmployeeCode() == null ? "-" : e.getEmployeeCode()));
        // Remaining (unadjusted) advance the employee still owes, shown right after the code.
        v.put("remainingAdvance", support.money(e.getAdvanceBalance() == null ? BigDecimal.ZERO : e.getAdvanceBalance()));
        v.put("employeeType", e.getPaymentType() == PaymentType.MONTHLY ? "Monthly" : "Piece Rate");
        v.put("joiningDate", e.getJoiningDate() == null ? "-" : e.getJoiningDate().format(DAY));
        return v;
    }

    private void row(StringBuilder sb, String label, BigDecimal amount) {
        sb.append("<tr><td>").append(support.esc(label)).append("</td><td class=\"num\">")
                .append(support.money(amount)).append("</td></tr>");
    }

    private String days(double d) {
        return (d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d)) + " days";
    }

    private String monthlyStatus(PaySlipResponse slip) {
        if (slip.getDue() != null && slip.getDue().signum() <= 0) return "Paid";
        if (slip.getPaid() != null && slip.getPaid().signum() > 0) return "Partially Paid";
        return "Unpaid";
    }

    private Employee findEmployee(UUID id) {
        return employeeRepository.findById(id).orElseThrow(() -> new BusinessException("Employee not found"));
    }
}
