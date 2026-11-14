package com.skr.erp.service;

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
import com.skr.erp.entity.EmployeePayslip;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.EmployeeLedgerRepository;
import com.skr.erp.repository.EmployeePayslipRepository;
import com.skr.erp.repository.EmployeeRepository;
import com.skr.erp.repository.ProductionEntryDetailRepository;
import com.skr.erp.repository.SystemSettingRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Employee payroll account: advances, expenses, settlements and the payroll view. Monthly salary
 * slips come from {@link PayslipGenerationService} (saved history + live current month).
 */
@Service
@RequiredArgsConstructor
public class EmployeeLedgerService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final List<LedgerEntryType> SETTLEMENT_TYPES =
            List.of(LedgerEntryType.PAYOUT, LedgerEntryType.ADVANCE_ADJUSTMENT);
    private static final int MAX_PAGE_SIZE = 500;

    private final EmployeeRepository employeeRepository;
    private final EmployeeLedgerRepository ledgerRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PayslipGenerationService payslips;

    // Salary side of the payroll view (one year of monthly slips, or piece-rate totals).
    @Builder
    private record SalarySummary(List<PaySlipResponse> slips, BigDecimal earned, BigDecimal currentMonthAccrued,
                                 BigDecimal pendingSettlement, double leaveThisMonth, int year, List<Integer> years) {}

    // Record a single advance (period null) or a slip settlement (period = that month).
    public PayrollResponse recordEntry(UUID employeeId, RecordLedgerRequest request) {
        Employee employee = findEmployee(employeeId);
        LocalDate period = firstOfMonth(request.getPeriod());
        validateSettlement(employee, request, period);
        EmployeeLedger entry = new EmployeeLedger();
        entry.setEmployee(employee);
        entry.setEntryDate(request.getEntryDate());
        entry.setType(request.getType());
        entry.setAmount(request.getAmount());
        entry.setMode(request.getMode());
        entry.setNote(request.getNote());
        entry.setPeriod(period);
        ledgerRepository.save(entry);
        applyBalanceDelta(employee, balanceDelta(request.getType(), request.getAmount()));
        // Keep the saved slip's paid/due snapshot in step (locks the month once fully paid).
        if (period != null && isSettlement(request.getType())) payslips.syncPayment(employeeId, period);
        return getPayroll(employeeId);
    }

    // Advance/expense raise the balance; settling from advance lowers it; direct payment leaves it.
    private BigDecimal balanceDelta(LedgerEntryType type, BigDecimal amount) {
        if (type == LedgerEntryType.ADVANCE || type == LedgerEntryType.SPEND) return amount;
        if (type == LedgerEntryType.ADVANCE_ADJUSTMENT) return amount.negate();
        return BigDecimal.ZERO;
    }

    private void applyBalanceDelta(Employee employee, BigDecimal delta) {
        if (delta.signum() == 0) return;
        employee.setAdvanceBalance(nz(employee.getAdvanceBalance()).add(delta));
        employeeRepository.save(employee);
    }

    // Record the onboarding advance as the first ADVANCE ledger entry (one column for the balance).
    public void seedOpeningAdvance(Employee employee, BigDecimal amount, LocalDate date) {
        if (amount == null || amount.signum() <= 0) return;
        EmployeeLedger entry = new EmployeeLedger();
        entry.setEmployee(employee);
        entry.setEntryDate(date != null ? date : LocalDate.now());
        entry.setType(LedgerEntryType.ADVANCE);
        entry.setAmount(amount);
        entry.setNote("Opening advance");
        ledgerRepository.save(entry);
        applyBalanceDelta(employee, amount);
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
        if (!entryId.equals(latestIdOfType(employeeId, entry.getType()))) {
            throw new BusinessException("Only the latest entry can be deleted");
        }
        ledgerRepository.delete(entry);
        applyBalanceDelta(entry.getEmployee(), entry.getAmount().negate());
        return getPayroll(employeeId);
    }

    // A settlement (direct payment or from advance) is only valid for a completed month, within its due.
    private void validateSettlement(Employee employee, RecordLedgerRequest request, LocalDate period) {
        if (!isSettlement(request.getType())) return;
        BigDecimal due = isMonthly(employee) ? monthlyDue(employee, period) : pieceDue(employee.getId());
        if (request.getAmount().compareTo(due) > 0) throw new BusinessException("Amount exceeds the balance due");
        if (request.getType() == LedgerEntryType.ADVANCE_ADJUSTMENT
                && request.getAmount().compareTo(nz(employee.getAdvanceBalance())) > 0) {
            throw new BusinessException("Amount exceeds the available advance balance");
        }
    }

    // Due of a completed month's saved slip (monthly staff) — the month is required and must be past.
    private BigDecimal monthlyDue(Employee employee, LocalDate period) {
        if (period == null) throw new BusinessException("Settlement must specify the month");
        if (!YearMonth.from(period).isBefore(YearMonth.now())) {
            throw new BusinessException("The current or a future month cannot be settled yet");
        }
        return payslips.dueForSettlement(employee, period);
    }

    // Total outstanding piece earnings (piece-rate staff) — settled directly, not month-bound.
    private BigDecimal pieceDue(UUID employeeId) {
        Map<LedgerEntryType, BigDecimal> sums = sumsByType(employeeId);
        BigDecimal earned = nz(productionEntryDetailRepository.sumEarningByEmployee(employeeId));
        return earned.subtract(sum(sums, LedgerEntryType.PAYOUT)).subtract(sum(sums, LedgerEntryType.ADVANCE_ADJUSTMENT));
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
            applyBalanceDelta(employee, item.getAmount());
        }
        return request.getItems().size();
    }

    @Transactional
    public PayrollResponse getPayroll(UUID employeeId) {
        return getPayroll(employeeId, new PayrollQuery());
    }

    // Payroll view: one year of monthly slips (or piece totals) + overview totals + a page of transactions.
    @Transactional
    public PayrollResponse getPayroll(UUID employeeId, PayrollQuery query) {
        Employee employee = findEmployee(employeeId);
        // Save any completed month not stored yet (monthly staff); history is then only read back.
        payslips.ensureGenerated(employee);
        if (autoSettleEnabled()) autoSettle(employee);
        Map<LedgerEntryType, BigDecimal> sums = sumsByType(employeeId);
        int year = query.getYear() != null ? query.getYear() : Year.now().getValue();
        SalarySummary salary = isMonthly(employee) ? monthlySummary(employee, year) : pieceSummary(employeeId, sums);
        return toPayrollResponse(employee, sums, salary, transactionLog(employeeId, query));
    }

    // Monthly staff: the year's month rows + all-time totals (saved months + live current month).
    private SalarySummary monthlySummary(Employee employee, int year) {
        PayslipGenerationService.MonthCalc current = payslips.currentMonth(employee);
        EmployeePayslipRepository.Totals saved = payslips.savedTotals(employee.getId());
        return SalarySummary.builder()
                .slips(payslips.slipsForYear(employee, year, current))
                .earned(nz(saved.getEarned()).add(current.earned()))
                .currentMonthAccrued(current.earned())
                .pendingSettlement(nz(saved.getDue()))
                .leaveThisMonth(current.leaveDays().doubleValue())
                .year(year).years(payslips.yearsFor(employee)).build();
    }

    // Piece-rate staff: earnings come from recorded work; no monthly slips.
    private SalarySummary pieceSummary(UUID employeeId, Map<LedgerEntryType, BigDecimal> sums) {
        YearMonth now = YearMonth.now();
        BigDecimal earned = nz(productionEntryDetailRepository.sumEarningByEmployee(employeeId));
        BigDecimal settled = sum(sums, LedgerEntryType.PAYOUT).add(sum(sums, LedgerEntryType.ADVANCE_ADJUSTMENT));
        BigDecimal thisMonth = productionEntryDetailRepository.sumEarningByEmployeeBetween(employeeId, now.atDay(1), now.atEndOfMonth());
        return SalarySummary.builder().slips(List.of()).earned(earned)
                .currentMonthAccrued(nz(thisMonth))
                .pendingSettlement(earned.subtract(settled).max(BigDecimal.ZERO))
                .year(now.getYear()).years(List.of()).build();
    }

    private PayrollResponse toPayrollResponse(Employee employee, Map<LedgerEntryType, BigDecimal> sums,
                                              SalarySummary salary, Page<EmployeeLedger> log) {
        BigDecimal advanceGiven = sum(sums, LedgerEntryType.ADVANCE).add(sum(sums, LedgerEntryType.SPEND));
        BigDecimal payouts = sum(sums, LedgerEntryType.PAYOUT);
        return PayrollResponse.builder()
                .paymentType(employee.getPaymentType()).monthlySalary(nz(employee.getMonthlySalary()))
                .totalEarned(salary.earned()).totalAdvanceGiven(advanceGiven)
                .advanceBalance(nz(employee.getAdvanceBalance())).totalPaid(payouts)
                .advanceAdjusted(sum(sums, LedgerEntryType.ADVANCE_ADJUSTMENT))
                .currentMonthAccrued(salary.currentMonthAccrued())
                .pending(salary.earned().subtract(advanceGiven).subtract(payouts))
                .pendingSettlement(salary.pendingSettlement()).leaveDaysThisMonth(salary.leaveThisMonth())
                .year(salary.year()).availableYears(salary.years())
                .slips(salary.slips()).slipTotal(salary.slips().size())
                .transactions(toLogResponses(employee.getId(), log.getContent())).txnTotal(log.getTotalElements())
                .build();
    }

    // When the auto-settle setting is on, completed-month dues are settled from the advance balance.
    private void autoSettle(Employee employee) {
        if (isMonthly(employee)) autoAdjustPastMonths(employee);
        else autoAdjustPiece(employee);
    }

    private boolean autoSettleEnabled() {
        return systemSettingRepository.findBySettingKey("PAYROLL_AUTO_SETTLE")
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue())).orElse(false);
    }

    // Monthly: settle each open saved month, oldest first, from the advance pool.
    private void autoAdjustPastMonths(Employee employee) {
        BigDecimal pool = nz(employee.getAdvanceBalance());
        BigDecimal consumed = BigDecimal.ZERO;
        for (EmployeePayslip slip : pool.signum() > 0 ? payslips.openDueSlips(employee.getId()) : List.<EmployeePayslip>of()) {
            if (pool.signum() <= 0) break;
            BigDecimal apply = slip.getDue().min(pool);
            saveAdjustment(employee, YearMonth.from(slip.getPeriod()), apply);
            payslips.syncPayment(employee.getId(), slip.getPeriod());
            pool = pool.subtract(apply);
            consumed = consumed.add(apply);
        }
        applyBalanceDelta(employee, consumed.negate());
    }

    // Piece-rate: auto-settle completed-month earnings (excludes the in-progress month) from advance.
    private void autoAdjustPiece(Employee employee) {
        BigDecimal pool = nz(employee.getAdvanceBalance());
        if (pool.signum() <= 0) return;
        YearMonth prev = YearMonth.now().minusMonths(1);
        LocalDate start = employee.getJoiningDate() != null ? employee.getJoiningDate() : LocalDate.of(1970, 1, 1);
        BigDecimal completedEarned = nz(productionEntryDetailRepository
                .sumEarningByEmployeeBetween(employee.getId(), start, prev.atEndOfMonth()));
        Map<LedgerEntryType, BigDecimal> sums = sumsByType(employee.getId());
        BigDecimal due = completedEarned.subtract(sum(sums, LedgerEntryType.PAYOUT))
                .subtract(sum(sums, LedgerEntryType.ADVANCE_ADJUSTMENT));
        if (due.signum() <= 0) return;
        BigDecimal apply = due.min(pool);
        saveAdjustment(employee, prev, apply);
        applyBalanceDelta(employee, apply.negate());
    }

    // One advance-adjustment entry settling a month, dated at that month's end.
    private void saveAdjustment(Employee employee, YearMonth month, BigDecimal amount) {
        EmployeeLedger adj = new EmployeeLedger();
        adj.setEmployee(employee);
        adj.setEntryDate(month.atEndOfMonth());
        adj.setType(LedgerEntryType.ADVANCE_ADJUSTMENT);
        adj.setAmount(amount);
        adj.setPeriod(month.atDay(1));
        adj.setNote(month.atDay(1).format(MONTH));
        ledgerRepository.save(adj);
    }

    // One month's slip for the pay-slip PDF (live current month, else the saved slip).
    public PaySlipResponse slipForMonth(UUID employeeId, LocalDate period) {
        return payslips.slipFor(findEmployee(employeeId), YearMonth.from(period));
    }

    // Latest settlement date for a month's slip (null when the month has not been settled yet).
    public LocalDate settlementDateForMonth(UUID employeeId, LocalDate period) {
        return ledgerRepository.lastSettlementDate(employeeId, firstOfMonth(period));
    }

    // One settlement/payment entry (validated to belong to the employee) — used by the piece pay-slip PDF.
    public EmployeeLedger settlementEntry(UUID employeeId, UUID entryId) {
        EmployeeLedger entry = ledgerRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));
        if (!entry.getEmployee().getId().equals(employeeId)) throw new BusinessException("Transaction not found");
        return entry;
    }

    // One filtered, newest-first page of the transaction log (filtered and paged in the database).
    private Page<EmployeeLedger> transactionLog(UUID employeeId, PayrollQuery query) {
        int size = Math.min(Math.max(query.getTxnSize(), 1), MAX_PAGE_SIZE);
        Sort newestFirst = Sort.by(Sort.Order.desc("entryDate"), Sort.Order.desc("createdAt"));
        PageRequest page = PageRequest.of(Math.max(query.getTxnPage(), 0), size, newestFirst);
        return ledgerRepository.findAll(logFilter(employeeId, query), page);
    }

    // Employee + optional type (SETTLEMENT = payout + advance adjustment) + optional entry-date range.
    private Specification<EmployeeLedger> logFilter(UUID employeeId, PayrollQuery q) {
        return (root, cq, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("employee").get("id"), employeeId));
            String type = q.getTxnType();
            if (type != null && !type.isBlank()) where.add(root.get("type").in(typesFor(type)));
            if (q.getTxnFrom() != null) where.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("entryDate"), q.getTxnFrom()));
            if (q.getTxnTo() != null) where.add(cb.lessThanOrEqualTo(root.<LocalDate>get("entryDate"), q.getTxnTo()));
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    // Entry types behind a filter value; an unknown value matches nothing.
    private List<LedgerEntryType> typesFor(String type) {
        if ("SETTLEMENT".equals(type)) return SETTLEMENT_TYPES;
        for (LedgerEntryType t : LedgerEntryType.values()) {
            if (t.name().equals(type)) return List.of(t);
        }
        return List.of();
    }

    // Transaction rows; only the newest advance and the newest expense are deletable.
    private List<LedgerEntryResponse> toLogResponses(UUID employeeId, List<EmployeeLedger> entries) {
        UUID latestAdvance = latestIdOfType(employeeId, LedgerEntryType.ADVANCE);
        UUID latestSpend = latestIdOfType(employeeId, LedgerEntryType.SPEND);
        return entries.stream()
                .map(t -> toResponse(t, t.getId().equals(latestAdvance) || t.getId().equals(latestSpend)))
                .toList();
    }

    // This month's net earned salary — used by the employee list/detail current-month figure.
    public BigDecimal currentMonthSalaryPayable(Employee employee) {
        return payslips.currentMonth(employee).earned();
    }

    // Money given to one employee (advance + expense + direct payment) — one grouped read.
    public BigDecimal moneyGivenForEmployee(Employee employee) {
        Map<LedgerEntryType, BigDecimal> sums = sumsByType(employee.getId());
        return sum(sums, LedgerEntryType.ADVANCE).add(sum(sums, LedgerEntryType.SPEND))
                .add(sum(sums, LedgerEntryType.PAYOUT));
    }

    // Overall payroll (all employees) = advances + expenses + payments ever given.
    public BigDecimal totalMoneyGivenAll() {
        return nz(ledgerRepository.sumByTypes(List.of(
                LedgerEntryType.ADVANCE, LedgerEntryType.SPEND, LedgerEntryType.PAYOUT)));
    }

    // Money given grouped by employee (one query) — for the scalable employee list.
    public Map<UUID, BigDecimal> moneyGivenGrouped() {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] r : ledgerRepository.sumMoneyGivenGroupedByEmployee()) {
            map.put((UUID) r[0], r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1]);
        }
        return map;
    }

    // Cumulative salary earned from joining to the current month (monthly employees only).
    public BigDecimal totalAccruedSalary(Employee employee) {
        if (!isMonthly(employee)) return BigDecimal.ZERO;
        return payslips.totals(employee).totalEarned();
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

    // Per-type ledger totals for one employee (one grouped query).
    private Map<LedgerEntryType, BigDecimal> sumsByType(UUID employeeId) {
        Map<LedgerEntryType, BigDecimal> sums = new EnumMap<>(LedgerEntryType.class);
        for (Object[] r : ledgerRepository.sumByTypeForEmployee(employeeId)) {
            sums.put((LedgerEntryType) r[0], (BigDecimal) r[1]);
        }
        return sums;
    }

    private BigDecimal sum(Map<LedgerEntryType, BigDecimal> sums, LedgerEntryType type) {
        return sums.getOrDefault(type, BigDecimal.ZERO);
    }

    private UUID latestIdOfType(UUID employeeId, LedgerEntryType type) {
        return ledgerRepository.findFirstByEmployeeIdAndTypeOrderByEntryDateDescCreatedAtDesc(employeeId, type)
                .map(EmployeeLedger::getId).orElse(null);
    }

    private LedgerEntryResponse toResponse(EmployeeLedger entry, boolean deletable) {
        return LedgerEntryResponse.builder()
                .id(entry.getId()).entryDate(entry.getEntryDate())
                .type(entry.getType()).amount(entry.getAmount())
                .mode(entry.getMode()).note(entry.getNote()).period(entry.getPeriod())
                .deletable(deletable).build();
    }

    private boolean isSettlement(LedgerEntryType type) {
        return SETTLEMENT_TYPES.contains(type);
    }

    private boolean isMonthly(Employee employee) {
        return employee.getPaymentType() == PaymentType.MONTHLY;
    }

    // Slip months are always stored as the first day of the month.
    private LocalDate firstOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(1);
    }

    private Employee findEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found"));
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
