package com.skr.erp.service;

import com.skr.erp.common.constants.AttendanceStatus;
import com.skr.erp.common.constants.PaymentType;
import com.skr.erp.common.constants.PayslipMonthStatus;
import com.skr.erp.common.constants.PayslipPaymentStatus;
import com.skr.erp.dto.response.PaySlipResponse;
import com.skr.erp.entity.Employee;
import com.skr.erp.entity.EmployeeAttendance;
import com.skr.erp.entity.EmployeePayslip;
import com.skr.erp.entity.EmployeeWorkPeriod;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.EmployeeAttendanceRepository;
import com.skr.erp.repository.EmployeeLedgerRepository;
import com.skr.erp.repository.EmployeePayslipRepository;
import com.skr.erp.repository.EmployeeRepository;
import com.skr.erp.repository.EmployeeWorkPeriodRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Monthly-staff salary slips: day-level active/inactive maths, saving completed months (idempotent
 * catch-up), in-place refresh of unfinalized months, payment snapshots and the paid-month lock.
 * Inactive spell = [startDate, endDate): the inactive date is unpaid, the reactivation date is paid.
 */
@Service
@RequiredArgsConstructor
public class PayslipGenerationService {

    // Fixed English labels ("Sep 2026") regardless of the machine's locale.
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final EmployeePayslipRepository payslipRepository;
    private final EmployeeWorkPeriodRepository workPeriodRepository;
    private final EmployeeAttendanceRepository attendanceRepository;
    private final EmployeeLedgerRepository ledgerRepository;
    private final EmployeeRepository employeeRepository;
    private final PlatformTransactionManager transactionManager;

    /** One month's salary calculation. */
    @Builder
    public record MonthCalc(YearMonth month, PayslipMonthStatus status, int activeDays, int monthDays,
                            BigDecimal monthlySalary, BigDecimal base, BigDecimal leaveDays,
                            BigDecimal leaveDeduction, BigDecimal earned) {}

    /** All-time earned salary (saved months + live current month) and the current month's salary. */
    public record SalaryTotals(BigDecimal totalEarned, BigDecimal currentMonth) {}

    private record SalaryInputs(Employee employee, List<EmployeeWorkPeriod> spells,
                                Map<YearMonth, List<EmployeeAttendance>> leaveByMonth) {}

    private record DateWindow(LocalDate from, LocalDate to) {}

    // ── Generation ──────────────────────────────────────────────────────────

    /** Saves every missing completed month (joining month → last month); safe to call repeatedly. */
    public void ensureGenerated(Employee employee) {
        List<YearMonth> missing = missingMonths(employee);
        if (missing.isEmpty()) return;
        try {
            inNewTransaction().executeWithoutResult(status -> saveMonths(employee, missing));
        } catch (DataIntegrityViolationException alreadySaved) {
            // A parallel request saved these months first; the unique key keeps one row per month.
        }
    }

    // Completed months between joining and last month that have no saved slip yet.
    private List<YearMonth> missingMonths(Employee employee) {
        YearMonth last = YearMonth.now().minusMonths(1);
        if (!isMonthly(employee) || firstMonth(employee).isAfter(last)) return List.of();
        Set<LocalDate> saved = new HashSet<>(payslipRepository.findPeriodsByEmployeeId(employee.getId()));
        return monthsBetween(firstMonth(employee), last).stream()
                .filter(m -> !saved.contains(m.atDay(1))).toList();
    }

    // Calculates and inserts the given completed months in one batch.
    private void saveMonths(Employee employee, List<YearMonth> months) {
        YearMonth first = months.get(0), last = months.get(months.size() - 1);
        SalaryInputs inputs = inputs(employee, first, last);
        Map<LocalDate, BigDecimal> settled = settledByPeriod(employee.getId(), first.atDay(1), last.atDay(1));
        Employee ref = employeeRepository.getReferenceById(employee.getId());
        BigDecimal salary = nz(employee.getMonthlySalary());
        List<EmployeePayslip> rows = new ArrayList<>();
        for (YearMonth month : months) {
            rows.add(newRow(ref, calculate(inputs, month, salary), settled.getOrDefault(month.atDay(1), BigDecimal.ZERO)));
        }
        payslipRepository.saveAllAndFlush(rows);
    }

    private EmployeePayslip newRow(Employee ref, MonthCalc calc, BigDecimal settled) {
        EmployeePayslip row = new EmployeePayslip();
        row.setEmployee(ref);
        row.setPeriod(calc.month().atDay(1));
        applyCalc(row, calc);
        applyPaid(row, settled);
        lockIfFullyPaid(row);
        return row;
    }

    // ── Reads ───────────────────────────────────────────────────────────────

    /** This month's salary — always calculated fresh, never saved. */
    public MonthCalc currentMonth(Employee employee) {
        YearMonth now = YearMonth.now();
        return calculate(inputs(employee, now, now), now, nz(employee.getMonthlySalary()));
    }

    /** All-time earned and current-month salary for one monthly employee. */
    public SalaryTotals totals(Employee employee) {
        ensureGenerated(employee);
        BigDecimal current = currentMonth(employee).earned();
        BigDecimal saved = nz(payslipRepository.totalsByEmployee(employee.getId()).getEarned());
        return new SalaryTotals(saved.add(current), current);
    }

    /** All-time earned and unsettled due of the saved (completed) months. */
    public EmployeePayslipRepository.Totals savedTotals(UUID employeeId) {
        return payslipRepository.totalsByEmployee(employeeId);
    }

    /** Salary totals for many monthly employees with a fixed number of queries (employee list). */
    public Map<UUID, SalaryTotals> totalsFor(List<Employee> employees) {
        if (employees.isEmpty()) return Map.of();
        List<UUID> ids = employees.stream().map(Employee::getId).toList();
        Map<UUID, Object[]> stats = savedStats(ids);
        List<Employee> stale = employees.stream().filter(e -> isStale(e, stats.get(e.getId()))).toList();
        stale.forEach(this::ensureGenerated);
        Map<UUID, Object[]> fresh = stale.isEmpty() ? stats : savedStats(ids);
        Map<UUID, MonthCalc> current = currentMonths(employees);
        Map<UUID, SalaryTotals> totals = new HashMap<>();
        for (Employee e : employees) {
            BigDecimal month = current.get(e.getId()).earned();
            Object[] row = fresh.get(e.getId());
            BigDecimal saved = row == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            totals.put(e.getId(), new SalaryTotals(saved.add(month), month));
        }
        return totals;
    }

    // Saved-month count and earned total per employee, keyed by employee id.
    private Map<UUID, Object[]> savedStats(List<UUID> ids) {
        Map<UUID, Object[]> stats = new HashMap<>();
        for (Object[] r : payslipRepository.countAndEarnedByEmployees(ids)) stats.put((UUID) r[0], r);
        return stats;
    }

    // True when some completed month of the employee is not saved yet.
    private boolean isStale(Employee employee, Object[] stats) {
        YearMonth first = firstMonth(employee), last = YearMonth.now().minusMonths(1);
        if (!isMonthly(employee) || first.isAfter(last)) return false;
        long saved = stats == null ? 0 : ((Number) stats[1]).longValue();
        return saved != monthsBetween(first, last).size();
    }

    // Live current-month salary for many employees from two batched reads.
    private Map<UUID, MonthCalc> currentMonths(List<Employee> employees) {
        YearMonth now = YearMonth.now();
        List<UUID> ids = employees.stream().map(Employee::getId).toList();
        Map<UUID, List<EmployeeWorkPeriod>> spells = workPeriodRepository.findByEmployeeIdIn(ids).stream()
                .collect(Collectors.groupingBy(p -> p.getEmployee().getId()));
        Map<UUID, List<EmployeeAttendance>> leave = attendanceRepository
                .findByEmployeeIdInAndDateBetween(ids, now.atDay(1), now.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));
        Map<UUID, MonthCalc> result = new HashMap<>();
        for (Employee e : employees) {
            SalaryInputs inputs = new SalaryInputs(e, spells.getOrDefault(e.getId(), List.of()),
                    Map.of(now, leave.getOrDefault(e.getId(), List.of())));
            result.put(e.getId(), calculate(inputs, now, nz(e.getMonthlySalary())));
        }
        return result;
    }

    /** One calendar year of month rows (saved months + live current month), newest first. */
    public List<PaySlipResponse> slipsForYear(Employee employee, int year, MonthCalc current) {
        List<PaySlipResponse> rows = new ArrayList<>();
        YearMonth now = YearMonth.now();
        if (year == now.getYear() && !firstMonth(employee).isAfter(now)) rows.add(currentSlip(employee, current));
        payslipRepository.findByEmployeeIdAndPeriodBetweenOrderByPeriodDesc(
                        employee.getId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .forEach(p -> rows.add(toResponse(p, false)));
        return rows;
    }

    /** Calendar years that have payroll months: current year down to the joining year. */
    public List<Integer> yearsFor(Employee employee) {
        List<Integer> years = new ArrayList<>();
        for (int y = Year.now().getValue(); y >= firstMonth(employee).getYear(); y--) years.add(y);
        return years;
    }

    /** One month's slip: live for the current month, otherwise the saved slip. */
    public PaySlipResponse slipFor(Employee employee, YearMonth month) {
        YearMonth now = YearMonth.now();
        if (month.isAfter(now) || month.isBefore(firstMonth(employee))) {
            throw new BusinessException("No payslip exists for " + month.atDay(1).format(MONTH));
        }
        if (month.equals(now)) return currentSlip(employee, currentMonth(employee));
        ensureGenerated(employee);
        return payslipRepository.findByEmployeeIdAndPeriod(employee.getId(), month.atDay(1))
                .map(p -> toResponse(p, false))
                .orElseThrow(() -> new BusinessException("No payslip exists for " + month.atDay(1).format(MONTH)));
    }

    // The in-progress month as a transient (never saved, never locked) slip.
    private PaySlipResponse currentSlip(Employee employee, MonthCalc current) {
        EmployeePayslip row = new EmployeePayslip();
        row.setPeriod(current.month().atDay(1));
        applyCalc(row, current);
        LocalDate period = row.getPeriod();
        applyPaid(row, settledByPeriod(employee.getId(), period, period).getOrDefault(period, BigDecimal.ZERO));
        return toResponse(row, true);
    }

    // ── Payments ────────────────────────────────────────────────────────────

    /** Due of a completed month that can still be settled (active and not yet fully paid). */
    public BigDecimal dueForSettlement(Employee employee, LocalDate period) {
        ensureGenerated(employee);
        String label = period.format(MONTH);
        EmployeePayslip slip = payslipRepository.findByEmployeeIdAndPeriod(employee.getId(), period)
                .orElseThrow(() -> new BusinessException("No payslip exists for " + label));
        if (slip.getMonthStatus() == PayslipMonthStatus.INACTIVE) {
            throw new BusinessException("No salary is due for " + label + " — the employee was inactive all month");
        }
        if (slip.isFinalized()) throw new BusinessException("Salary for " + label + " is already fully paid");
        return slip.getDue();
    }

    /** Saved months with money still due, oldest first (advance auto-settle order). */
    public List<EmployeePayslip> openDueSlips(UUID employeeId) {
        return payslipRepository.findByEmployeeIdAndFinalizedFalseAndDueGreaterThanOrderByPeriodAsc(employeeId, BigDecimal.ZERO);
    }

    /** Re-snapshots a saved month's paid/due after a settlement; locks it once fully paid. */
    @Transactional
    public void syncPayment(UUID employeeId, LocalDate period) {
        payslipRepository.findByEmployeeIdAndPeriod(employeeId, period)
                .filter(row -> !row.isFinalized())
                .ifPresent(row -> {
                    applyPaid(row, settledByPeriod(employeeId, period, period).getOrDefault(period, BigDecimal.ZERO));
                    lockIfFullyPaid(row);
                    payslipRepository.save(row);
                });
    }

    // ── Changes & locking ───────────────────────────────────────────────────

    /** Recalculates saved, unfinalized months in [from, to] keeping each month's salary snapshot. */
    @Transactional
    public void refresh(Employee employee, YearMonth from, YearMonth to) {
        List<EmployeePayslip> rows = payslipRepository.findByEmployeeIdAndFinalizedFalseAndPeriodBetweenOrderByPeriodAsc(
                employee.getId(), from.atDay(1), to.atDay(1));
        if (rows.isEmpty()) return;
        YearMonth first = YearMonth.from(rows.get(0).getPeriod());
        YearMonth last = YearMonth.from(rows.get(rows.size() - 1).getPeriod());
        SalaryInputs inputs = inputs(employee, first, last);
        Map<LocalDate, BigDecimal> settled = settledByPeriod(employee.getId(), first.atDay(1), last.atDay(1));
        for (EmployeePayslip row : rows) {
            applyCalc(row, calculate(inputs, YearMonth.from(row.getPeriod()), row.getMonthlySalary()));
            applyPaid(row, settled.getOrDefault(row.getPeriod(), BigDecimal.ZERO));
            lockIfFullyPaid(row);
        }
        payslipRepository.saveAll(rows);
    }

    /** First date a change may take effect on: the day after the newest fully paid month (null if none). */
    public LocalDate firstUnlockedDate(UUID employeeId) {
        return payslipRepository.findTopByEmployeeIdAndFinalizedTrueOrderByPeriodDesc(employeeId)
                .map(p -> YearMonth.from(p.getPeriod()).atEndOfMonth().plusDays(1))
                .orElse(null);
    }

    /** Rejects attendance changes inside a month whose salary is fully paid (locked). */
    public void assertMonthsOpen(UUID employeeId, LocalDate from, LocalDate to) {
        if (payslipRepository.existsByEmployeeIdAndFinalizedTrueAndPeriodBetween(
                employeeId, YearMonth.from(from).atDay(1), YearMonth.from(to).atDay(1))) {
            throw new BusinessException("Salary for this month is fully paid and locked; attendance can no longer be changed");
        }
    }

    /** Drops every saved month (joining date changed); refused once any month is paid and locked. */
    @Transactional
    public void resetSavedMonths(UUID employeeId) {
        if (payslipRepository.existsByEmployeeIdAndFinalizedTrue(employeeId)) {
            throw new BusinessException("Joining date cannot be changed after a salary month has been fully paid");
        }
        payslipRepository.deleteAllByEmployeeId(employeeId);
    }

    // ── Calculation ─────────────────────────────────────────────────────────

    // One month's salary: per-day salary × active days, minus leave taken on active days.
    private MonthCalc calculate(SalaryInputs inputs, YearMonth month, BigDecimal salary) {
        int monthDays = month.lengthOfMonth();
        DateWindow window = window(inputs.employee(), month);
        int active = window == null ? 0 : activeDays(inputs.spells(), window);
        if (active == 0) return emptyMonth(month, salary, window == null);
        BigDecimal leave = leaveOnActiveDays(inputs, month, window);
        BigDecimal perDay = salary.divide(BigDecimal.valueOf(monthDays), 2, RoundingMode.HALF_UP);
        // Whole rupees, so the UI's rounded amount matches the due exactly.
        BigDecimal base = perDay.multiply(BigDecimal.valueOf(active)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal deduction = perDay.multiply(leave).setScale(0, RoundingMode.HALF_UP);
        return MonthCalc.builder().month(month).status(PayslipMonthStatus.ACTIVE)
                .activeDays(active).monthDays(monthDays).monthlySalary(salary)
                .base(base).leaveDays(leave).leaveDeduction(deduction)
                .earned(base.subtract(deduction).max(BigDecimal.ZERO)).build();
    }

    // A month with no working day: INACTIVE (or not started yet when the joining date is later).
    private MonthCalc emptyMonth(YearMonth month, BigDecimal salary, boolean notStarted) {
        BigDecimal zero = BigDecimal.ZERO;
        return MonthCalc.builder().month(month)
                .status(notStarted ? PayslipMonthStatus.ACTIVE : PayslipMonthStatus.INACTIVE)
                .activeDays(0).monthDays(month.lengthOfMonth()).monthlySalary(salary)
                .base(zero).leaveDays(zero.setScale(1, RoundingMode.HALF_UP)).leaveDeduction(zero).earned(zero).build();
    }

    // Earning days of a month: from joining (if later) to month end, or to today for the current month.
    private DateWindow window(Employee employee, YearMonth month) {
        LocalDate from = month.atDay(1), to = month.atEndOfMonth(), today = LocalDate.now();
        LocalDate join = employee.getJoiningDate();
        if (join != null && join.isAfter(from)) from = join;
        if (month.equals(YearMonth.from(today)) && today.isBefore(to)) to = today;
        return from.isAfter(to) ? null : new DateWindow(from, to);
    }

    // Days in the window outside every inactive spell.
    private int activeDays(List<EmployeeWorkPeriod> spells, DateWindow window) {
        int days = 0;
        for (LocalDate d = window.from(); !d.isAfter(window.to()); d = d.plusDays(1)) {
            if (!isInactive(spells, d)) days++;
        }
        return days;
    }

    // Leave counts only on days the employee was active (half-day = 0.5).
    private BigDecimal leaveOnActiveDays(SalaryInputs inputs, YearMonth month, DateWindow window) {
        double days = 0;
        for (EmployeeAttendance a : inputs.leaveByMonth().getOrDefault(month, List.of())) {
            LocalDate d = a.getDate();
            boolean counted = !d.isBefore(window.from()) && !d.isAfter(window.to()) && !isInactive(inputs.spells(), d);
            if (counted) days += a.getStatus() == AttendanceStatus.HALF_DAY ? 0.5 : 1.0;
        }
        return BigDecimal.valueOf(days).setScale(1, RoundingMode.HALF_UP);
    }

    // Inactive from the inactive date up to (not including) the reactivation date.
    private static boolean isInactive(List<EmployeeWorkPeriod> spells, LocalDate day) {
        for (EmployeeWorkPeriod spell : spells) {
            boolean started = !day.isBefore(spell.getStartDate());
            boolean notBack = spell.getEndDate() == null || day.isBefore(spell.getEndDate());
            if (started && notBack) return true;
        }
        return false;
    }

    // Spells and leave for a month range, each loaded once.
    private SalaryInputs inputs(Employee employee, YearMonth from, YearMonth to) {
        UUID id = employee.getId();
        Map<YearMonth, List<EmployeeAttendance>> leave = attendanceRepository
                .findByEmployeeIdAndDateBetween(id, from.atDay(1), to.atEndOfMonth()).stream()
                .collect(Collectors.groupingBy(a -> YearMonth.from(a.getDate())));
        return new SalaryInputs(employee, workPeriodRepository.findByEmployeeIdOrderByStartDateAsc(id), leave);
    }

    // Settled amount (payouts + advance adjustments) per slip month in one grouped read.
    private Map<LocalDate, BigDecimal> settledByPeriod(UUID employeeId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> settled = new HashMap<>();
        for (Object[] r : ledgerRepository.sumSettledByPeriod(employeeId, from, to)) {
            settled.put((LocalDate) r[0], (BigDecimal) r[1]);
        }
        return settled;
    }

    // ── Row mapping ─────────────────────────────────────────────────────────

    private void applyCalc(EmployeePayslip row, MonthCalc calc) {
        row.setMonthStatus(calc.status());
        row.setActiveDays(calc.activeDays());
        row.setMonthDays(calc.monthDays());
        row.setMonthlySalary(calc.monthlySalary());
        row.setBaseSalary(calc.base());
        row.setLeaveDays(calc.leaveDays());
        row.setLeaveDeduction(calc.leaveDeduction());
        row.setEarned(calc.earned());
    }

    // Paid/due snapshot from the month's settlements.
    private void applyPaid(EmployeePayslip row, BigDecimal paid) {
        row.setPaid(paid);
        row.setDue(row.getEarned().subtract(paid).max(BigDecimal.ZERO));
        row.setPaymentStatus(paymentStatus(row, paid));
    }

    // Inactive or zero-salary months have nothing to pay, so no payment status.
    private PayslipPaymentStatus paymentStatus(EmployeePayslip row, BigDecimal paid) {
        if (row.getMonthStatus() == PayslipMonthStatus.INACTIVE || row.getEarned().signum() == 0) return null;
        if (paid.signum() <= 0) return PayslipPaymentStatus.UNPAID;
        return paid.compareTo(row.getEarned()) >= 0 ? PayslipPaymentStatus.PAID : PayslipPaymentStatus.PARTIAL;
    }

    // A fully paid month is finalized and never changes again.
    private void lockIfFullyPaid(EmployeePayslip row) {
        if (row.isFinalized() || row.getPaymentStatus() != PayslipPaymentStatus.PAID) return;
        row.setFinalized(true);
        row.setFinalizedAt(LocalDateTime.now());
    }

    private PaySlipResponse toResponse(EmployeePayslip p, boolean current) {
        boolean active = p.getMonthStatus() == PayslipMonthStatus.ACTIVE;
        return PaySlipResponse.builder()
                .period(p.getPeriod()).monthLabel(p.getPeriod().format(MONTH))
                .monthStatus(p.getMonthStatus().name()).activeDays(p.getActiveDays()).monthDays(p.getMonthDays())
                .monthlySalary(p.getMonthlySalary()).baseSalary(p.getBaseSalary())
                .leaveDays(p.getLeaveDays().doubleValue()).leaveDeduction(p.getLeaveDeduction())
                .earned(p.getEarned()).paid(p.getPaid()).due(p.getDue())
                .status(p.getPaymentStatus() == null ? null : p.getPaymentStatus().name())
                .current(current).finalized(p.isFinalized())
                .payable(!current && active && p.getDue().signum() > 0)
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    // First payroll month: the joining month (current month when the joining date is unknown).
    private YearMonth firstMonth(Employee employee) {
        return employee.getJoiningDate() != null ? YearMonth.from(employee.getJoiningDate()) : YearMonth.now();
    }

    private static List<YearMonth> monthsBetween(YearMonth from, YearMonth to) {
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) months.add(m);
        return months;
    }

    private boolean isMonthly(Employee employee) {
        return employee.getPaymentType() == PaymentType.MONTHLY;
    }

    // Catch-up runs in its own transaction so it also works from read-only requests.
    private TransactionTemplate inNewTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
