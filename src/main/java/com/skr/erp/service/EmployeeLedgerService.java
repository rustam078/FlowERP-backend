package com.skr.erp.service;

import com.skr.erp.common.constants.AttendanceStatus;
import com.skr.erp.common.constants.LedgerEntryType;
import com.skr.erp.common.constants.PaymentType;
import com.skr.erp.dto.request.BulkAdvanceRequest;
import com.skr.erp.dto.request.PayrollQuery;
import com.skr.erp.dto.request.RecordLedgerRequest;
import com.skr.erp.dto.response.LedgerEntryResponse;
import com.skr.erp.dto.response.LedgerSummaryResponse;
import com.skr.erp.dto.response.PaySlipResponse;
import com.skr.erp.dto.response.PayrollResponse;
import com.skr.erp.entity.Employee;
import com.skr.erp.entity.EmployeeLedger;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.EmployeeAttendanceRepository;
import com.skr.erp.repository.EmployeeLedgerRepository;
import com.skr.erp.repository.EmployeeRepository;
import com.skr.erp.repository.ProductionEntryDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Employee payroll: monthly pay-slips (earned = gross − leave), advances, settlements and net pending.
 */
@Service
@RequiredArgsConstructor
public class EmployeeLedgerService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy");

    private final EmployeeRepository employeeRepository;
    private final EmployeeLedgerRepository ledgerRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final EmployeeAttendanceRepository employeeAttendanceRepository;

    private record MonthSalary(BigDecimal base, double leaveDays, BigDecimal leaveDeduction, BigDecimal earned) {}

    // Record a single advance (period null) or a slip settlement payout (period = that month).
    public PayrollResponse recordEntry(UUID employeeId, RecordLedgerRequest request) {
        Employee employee = findEmployee(employeeId);
        EmployeeLedger entry = new EmployeeLedger();
        entry.setEmployee(employee);
        entry.setEntryDate(request.getEntryDate());
        entry.setType(request.getType());
        entry.setAmount(request.getAmount());
        entry.setMode(request.getMode());
        entry.setNote(request.getNote());
        entry.setPeriod(request.getPeriod());
        ledgerRepository.save(entry);
        return getPayroll(employeeId);
    }

    // Only the latest advance / latest expense can be removed; payments & adjustments are permanent.
    public PayrollResponse deleteEntry(UUID employeeId, UUID entryId) {
        EmployeeLedger entry = ledgerRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));
        if (!entry.getEmployee().getId().equals(employeeId)) {
            throw new BusinessException("Transaction does not belong to this employee");
        }
        if (entry.getType() != LedgerEntryType.ADVANCE && entry.getType() != LedgerEntryType.SPEND) {
            throw new BusinessException("Only advances or expenses can be deleted");
        }
        boolean isLatest = ledgerRepository.findByEmployeeIdOrderByEntryDateDescCreatedAtDesc(employeeId).stream()
                .filter(t -> t.getType() == entry.getType()).findFirst()
                .map(t -> t.getId().equals(entryId)).orElse(false);
        if (!isLatest) {
            throw new BusinessException("Only the latest entry can be deleted");
        }
        ledgerRepository.delete(entry);
        return getPayroll(employeeId);
    }

    // Give a weekly expense (pocket money) to several employees at once — counts like advance.
    public int bulkAdvance(BulkAdvanceRequest request) {
        for (BulkAdvanceRequest.Item item : request.getItems()) {
            Employee employee = findEmployee(item.getEmployeeId());
            EmployeeLedger entry = new EmployeeLedger();
            entry.setEmployee(employee);
            entry.setEntryDate(request.getEntryDate());
            entry.setType(LedgerEntryType.SPEND);
            entry.setAmount(item.getAmount());
            entry.setMode(request.getMode());
            entry.setNote(request.getNote());
            ledgerRepository.save(entry);
        }
        return request.getItems().size();
    }

    @Transactional
    public PayrollResponse getPayroll(UUID employeeId) {
        return getPayroll(employeeId, new PayrollQuery());
    }

    @Transactional
    public PayrollResponse getPayroll(UUID employeeId, PayrollQuery query) {
        Employee employee = findEmployee(employeeId);
        List<EmployeeLedger> txns = ledgerRepository.findByEmployeeIdOrderByEntryDateDescCreatedAtDesc(employeeId);
        BigDecimal opening = nz(employee.getOpeningAdvance());
        boolean monthly = employee.getPaymentType() == PaymentType.MONTHLY;
        if (monthly) autoAdjustPastMonths(employee, txns, opening);

        BigDecimal advanceGiven = sum(txns, LedgerEntryType.ADVANCE).add(sum(txns, LedgerEntryType.SPEND)).add(opening);
        BigDecimal payouts = sum(txns, LedgerEntryType.PAYOUT);
        BigDecimal pool = advanceGiven.subtract(sum(txns, LedgerEntryType.ADVANCE_ADJUSTMENT));

        List<PaySlipResponse> slips = new ArrayList<>();
        BigDecimal earned;
        double leaveThisMonth = 0;
        if (monthly) {
            SlipBuild build = buildSlips(employee, txns, pool);
            slips = build.slips();
            earned = build.totalEarned();
            pool = build.poolAfterPreview();
            leaveThisMonth = build.leaveThisMonth();
        } else {
            earned = nz(productionEntryDetailRepository.sumEarningByEmployee(employeeId));
        }

        // Full transaction log (only newest advance & newest expense are deletable).
        UUID latestAdvance = latestIdOfType(txns, LedgerEntryType.ADVANCE);
        UUID latestSpend = latestIdOfType(txns, LedgerEntryType.SPEND);
        List<LedgerEntryResponse> log = new ArrayList<>();
        txns.forEach(t -> log.add(toResponse(t, t.getId().equals(latestAdvance) || t.getId().equals(latestSpend))));
        if (opening.signum() > 0) log.add(openingAdvanceRow(employee, opening));
        log.sort(Comparator.comparing(LedgerEntryResponse::getEntryDate, Comparator.nullsLast(Comparator.reverseOrder())));

        // Filter + paginate the transaction log and the slips (server-side).
        List<LedgerEntryResponse> filtered = log.stream().filter(e -> matches(e, query)).toList();

        return PayrollResponse.builder()
                .paymentType(employee.getPaymentType())
                .monthlySalary(nz(employee.getMonthlySalary()))
                .totalEarned(earned).totalAdvanceGiven(advanceGiven)
                .advanceBalance(pool.max(BigDecimal.ZERO)).totalPaid(payouts)
                .pending(earned.subtract(advanceGiven).subtract(payouts))
                .leaveDaysThisMonth(leaveThisMonth)
                .slips(page(slips, query.getSlipPage(), query.getSlipSize())).slipTotal(slips.size())
                .transactions(page(filtered, query.getTxnPage(), query.getTxnSize())).txnTotal(filtered.size())
                .build();
    }

    // A transaction matches the chosen type (SETTLEMENT = payout + auto-adjust) and date range.
    private boolean matches(LedgerEntryResponse e, PayrollQuery q) {
        String type = q.getTxnType();
        if (type != null && !type.isBlank()) {
            boolean ok = "SETTLEMENT".equals(type)
                    ? (e.getType() == LedgerEntryType.PAYOUT || e.getType() == LedgerEntryType.ADVANCE_ADJUSTMENT)
                    : type.equals(e.getType().name());
            if (!ok) return false;
        }
        if (q.getTxnFrom() != null && e.getEntryDate() != null && e.getEntryDate().isBefore(q.getTxnFrom())) return false;
        if (q.getTxnTo() != null && e.getEntryDate() != null && e.getEntryDate().isAfter(q.getTxnTo())) return false;
        return true;
    }

    private <T> List<T> page(List<T> all, int pageIndex, int size) {
        if (size <= 0) return all;
        int from = Math.min(Math.max(pageIndex, 0) * size, all.size());
        int to = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    private record SlipBuild(List<PaySlipResponse> slips, BigDecimal totalEarned, BigDecimal poolAfterPreview, double leaveThisMonth) {}

    // One slip per month (join→current); current month's due previews any unapplied advance.
    private SlipBuild buildSlips(Employee employee, List<EmployeeLedger> txns, BigDecimal pool) {
        YearMonth now = YearMonth.now();
        YearMonth ym = employee.getJoiningDate() != null ? YearMonth.from(employee.getJoiningDate()) : now;
        List<PaySlipResponse> slips = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        double leaveNow = 0;
        while (!ym.isAfter(now)) {
            MonthSalary ms = salaryBreakdown(employee, ym);
            if (ms.earned().signum() > 0 || ym.equals(now)) {
                boolean current = ym.equals(now);
                // Current (in-progress) month gets no advance applied — only completed months auto-settle.
                BigDecimal paid = paidForPeriod(txns, ym);
                if (current) leaveNow = ms.leaveDays();
                slips.add(toSlip(ym, ms, paid, current));
                total = total.add(ms.earned());
            }
            ym = ym.plusMonths(1);
        }
        slips.sort(Comparator.comparing(PaySlipResponse::getPeriod).reversed());
        return new SlipBuild(slips, total, pool, leaveNow);
    }

    private PaySlipResponse toSlip(YearMonth ym, MonthSalary ms, BigDecimal paid, boolean current) {
        BigDecimal due = ms.earned().subtract(paid).max(BigDecimal.ZERO);
        return PaySlipResponse.builder()
                .period(ym.atDay(1)).monthLabel(ym.atDay(1).format(MONTH))
                .baseSalary(ms.base()).leaveDays(ms.leaveDays()).leaveDeduction(ms.leaveDeduction())
                .earned(ms.earned()).paid(paid).due(due)
                .status(statusOf(ms.earned(), paid)).current(current).build();
    }

    // Auto-apply outstanding advance to any completed month still owed, as a permanent transaction.
    private void autoAdjustPastMonths(Employee employee, List<EmployeeLedger> txns, BigDecimal opening) {
        BigDecimal pool = sum(txns, LedgerEntryType.ADVANCE).add(sum(txns, LedgerEntryType.SPEND))
                .add(opening).subtract(sum(txns, LedgerEntryType.ADVANCE_ADJUSTMENT));
        if (pool.signum() <= 0) return;
        YearMonth now = YearMonth.now();
        YearMonth ym = employee.getJoiningDate() != null ? YearMonth.from(employee.getJoiningDate()) : now;
        while (ym.isBefore(now) && pool.signum() > 0) {
            MonthSalary ms = salaryBreakdown(employee, ym);
            BigDecimal due = ms.earned().subtract(paidForPeriod(txns, ym));
            if (due.signum() > 0) {
                BigDecimal apply = due.min(pool);
                EmployeeLedger adj = new EmployeeLedger();
                adj.setEmployee(employee);
                adj.setEntryDate(ym.atEndOfMonth());
                adj.setType(LedgerEntryType.ADVANCE_ADJUSTMENT);
                adj.setAmount(apply);
                adj.setPeriod(ym.atDay(1));
                adj.setNote(ym.atDay(1).format(MONTH));
                ledgerRepository.save(adj);
                txns.add(adj);
                pool = pool.subtract(apply);
            }
            ym = ym.plusMonths(1);
        }
    }

    // Salary breakdown for a month: base (days employed × per-day), leave deduction, net earned.
    private MonthSalary salaryBreakdown(Employee employee, YearMonth ym) {
        BigDecimal basic = nz(employee.getMonthlySalary());
        BigDecimal zero = BigDecimal.ZERO;
        if (basic.signum() == 0) return new MonthSalary(zero, 0, zero, zero);
        LocalDate monthStart = ym.atDay(1), monthEnd = ym.atEndOfMonth(), today = LocalDate.now();
        LocalDate join = employee.getJoiningDate();
        LocalDate start = (join != null && join.isAfter(monthStart)) ? join : monthStart;
        LocalDate end = ym.equals(YearMonth.now()) && today.isBefore(monthEnd) ? today : monthEnd;
        if (start.isAfter(end)) return new MonthSalary(zero, 0, zero, zero);
        long employedDays = ChronoUnit.DAYS.between(start, end) + 1;
        double leaveDays = employeeAttendanceRepository.findByEmployeeIdAndDateBetween(employee.getId(), start, end)
                .stream().mapToDouble(a -> a.getStatus() == AttendanceStatus.HALF_DAY ? 0.5 : 1.0).sum();
        BigDecimal perDay = basic.divide(BigDecimal.valueOf(ym.lengthOfMonth()), 2, RoundingMode.HALF_UP);
        BigDecimal base = perDay.multiply(BigDecimal.valueOf(employedDays));
        BigDecimal leaveDeduction = perDay.multiply(BigDecimal.valueOf(leaveDays));
        return new MonthSalary(base, leaveDays, leaveDeduction, base.subtract(leaveDeduction).max(zero));
    }

    // This month's net earned salary — used by the employee list/detail current-month figure.
    public BigDecimal currentMonthSalaryPayable(Employee employee) {
        return salaryBreakdown(employee, YearMonth.now()).earned();
    }

    public record PayrollTotals(BigDecimal moneyGiven, BigDecimal advanceBalance) {}

    // Per-employee totals in one ledger read: money given (advance+expense+payment) and advance balance.
    public PayrollTotals payrollTotals(UUID employeeId, BigDecimal opening) {
        List<EmployeeLedger> txns = ledgerRepository.findByEmployeeIdOrderByEntryDateDescCreatedAtDesc(employeeId);
        BigDecimal given = sum(txns, LedgerEntryType.ADVANCE).add(sum(txns, LedgerEntryType.SPEND)).add(nz(opening));
        BigDecimal moneyGiven = given.add(sum(txns, LedgerEntryType.PAYOUT));
        BigDecimal advanceBalance = given.subtract(sum(txns, LedgerEntryType.ADVANCE_ADJUSTMENT)).max(BigDecimal.ZERO);
        return new PayrollTotals(moneyGiven, advanceBalance);
    }

    // Overall payroll (all employees) = advances + expenses + payments ever given.
    public BigDecimal totalMoneyGivenAll() {
        return nz(ledgerRepository.sumByTypes(List.of(
                LedgerEntryType.ADVANCE, LedgerEntryType.SPEND, LedgerEntryType.PAYOUT)));
    }

    // Cumulative salary earned from joining to the current month (monthly employees only).
    public BigDecimal totalAccruedSalary(Employee employee) {
        if (employee.getPaymentType() != PaymentType.MONTHLY || nz(employee.getMonthlySalary()).signum() == 0) {
            return BigDecimal.ZERO;
        }
        YearMonth now = YearMonth.now();
        YearMonth ym = employee.getJoiningDate() != null ? YearMonth.from(employee.getJoiningDate()) : now;
        BigDecimal total = BigDecimal.ZERO;
        while (!ym.isAfter(now)) {
            total = total.add(salaryBreakdown(employee, ym).earned());
            ym = ym.plusMonths(1);
        }
        return total;
    }

    // Kept for the existing ledger card until the new payroll UI replaces it.
    @Transactional
    public LedgerSummaryResponse getLedger(UUID employeeId) {
        PayrollResponse p = getPayroll(employeeId);
        return LedgerSummaryResponse.builder()
                .paymentType(p.getPaymentType()).totalEarned(p.getTotalEarned())
                .totalAdvance(p.getAdvanceBalance()).totalPayout(p.getTotalPaid())
                .netPayable(p.getPending()).entries(p.getTransactions()).build();
    }

    private String statusOf(BigDecimal earned, BigDecimal paid) {
        if (paid.signum() <= 0) return "UNPAID";
        return paid.compareTo(earned) >= 0 ? "PAID" : "PARTIAL";
    }

    private BigDecimal sum(List<EmployeeLedger> txns, LedgerEntryType type) {
        return txns.stream().filter(t -> t.getType() == type)
                .map(EmployeeLedger::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal paidForPeriod(List<EmployeeLedger> txns, YearMonth ym) {
        LocalDate period = ym.atDay(1);
        return txns.stream()
                .filter(t -> (t.getType() == LedgerEntryType.PAYOUT || t.getType() == LedgerEntryType.ADVANCE_ADJUSTMENT)
                        && period.equals(t.getPeriod()))
                .map(EmployeeLedger::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LedgerEntryResponse openingAdvanceRow(Employee employee, BigDecimal opening) {
        return LedgerEntryResponse.builder()
                .id(null).entryDate(employee.getJoiningDate() != null ? employee.getJoiningDate() : LocalDate.now())
                .type(LedgerEntryType.ADVANCE).amount(opening).note("Opening advance").deletable(false).build();
    }

    private LedgerEntryResponse toResponse(EmployeeLedger entry, boolean deletable) {
        return LedgerEntryResponse.builder()
                .id(entry.getId()).entryDate(entry.getEntryDate())
                .type(entry.getType()).amount(entry.getAmount())
                .mode(entry.getMode()).note(entry.getNote()).period(entry.getPeriod())
                .deletable(deletable).build();
    }

    private UUID latestIdOfType(List<EmployeeLedger> txns, LedgerEntryType type) {
        return txns.stream().filter(t -> t.getType() == type)
                .map(EmployeeLedger::getId).findFirst().orElse(null);
    }

    private Employee findEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found"));
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
