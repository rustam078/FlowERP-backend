package com.skr.erp.service;

import com.skr.erp.common.constants.AttendanceStatus;
import com.skr.erp.dto.request.CreateEmployeeRequest;
import com.skr.erp.dto.request.MarkAttendanceRequest;
import com.skr.erp.dto.request.MarkAttendanceRangeRequest;
import com.skr.erp.dto.request.UpdateEmployeeRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.common.constants.PaymentType;
import com.skr.erp.entity.Employee;
import com.skr.erp.entity.EmployeeAttendance;
import com.skr.erp.entity.EmployeeWorkPeriod;
import com.skr.erp.entity.ProductRate;
import com.skr.erp.entity.ProductionEntry;
import com.skr.erp.entity.ProductionEntryDetail;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.EmployeeAttendanceRepository;
import com.skr.erp.repository.EmployeeRepository;
import com.skr.erp.repository.EmployeeWorkPeriodRepository;
import com.skr.erp.repository.ProductRateRepository;
import com.skr.erp.repository.ProductionEntryDetailRepository;
import com.skr.erp.repository.ProductionEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAttendanceRepository employeeAttendanceRepository;
    private final ProductionEntryRepository productionEntryRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final ProductRateRepository productRateRepository;
    private final EmployeeWorkPeriodRepository workPeriodRepository;
    private final EmployeeLedgerService employeeLedgerService;
    private final PayslipGenerationService payslips;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    public EmployeeResponse create(CreateEmployeeRequest request) {
        employeeRepository.findByEmail(request.getEmail()).ifPresent(emp -> {
            throw new BusinessException("Email already exists");
        });

        Employee employee = new Employee();
        employee.setEmployeeCode(generateEmployeeCode());
        employee.setFullName(request.getFullName());
        employee.setMobileNumber(request.getMobileNumber());
        employee.setEmail(request.getEmail());
        employee.setAddress(request.getAddress());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setActive(true);
        employee.setPaymentType(request.getPaymentType() != null ? request.getPaymentType() : PaymentType.PIECE_RATE);
        employee.setMonthlySalary(request.getMonthlySalary());
        employee = employeeRepository.save(employee);
        // Opening advance is recorded as the first advance ledger entry (sets the running balance).
        employeeLedgerService.seedOpeningAdvance(employee, request.getOpeningAdvance(), request.getJoiningDate());
        return map(employee);
    }

    // Active ↔ inactive is recorded as inactive spells; a same-status request is a no-op.
    private void applyActiveChange(Employee employee, Boolean active, LocalDate effectiveDate) {
        boolean wasActive = Boolean.TRUE.equals(employee.getActive());
        boolean nowActive = Boolean.TRUE.equals(active);
        if (wasActive == nowActive) return;
        LocalDate when = effectiveDate != null ? effectiveDate : LocalDate.now();
        if (nowActive) reactivateEmployee(employee, when);
        else deactivateEmployee(employee, when);
        employee.setActive(active);
    }

    // Opens an inactive spell from the inactive date (first non-working day).
    private void deactivateEmployee(Employee employee, LocalDate inactiveDate) {
        StatusWindowResponse window = statusWindow(employee);
        if (!"INACTIVATE".equals(window.getAction())) throw new BusinessException("Employee is already inactive");
        validateStatusDate(window, inactiveDate, "Inactive date");
        EmployeeWorkPeriod spell = new EmployeeWorkPeriod();
        spell.setEmployee(employee);
        spell.setStartDate(inactiveDate);
        workPeriodRepository.save(spell);
        payslips.refresh(employee, YearMonth.from(inactiveDate), YearMonth.now());
    }

    // Closes the open spell at the reactivation date (that day counts as a working day).
    private void reactivateEmployee(Employee employee, LocalDate reactivationDate) {
        EmployeeWorkPeriod spell = workPeriodRepository.findByEmployeeIdAndEndDateIsNull(employee.getId())
                .orElseThrow(() -> new BusinessException("Employee is already active"));
        validateStatusDate(statusWindow(employee), reactivationDate, "Reactivation date");
        // Delete leave records during inactive period
        employeeAttendanceRepository.deleteLeavesByEmployeeAndDateRange(employee.getId(), spell.getStartDate(), reactivationDate);
        spell.setEndDate(reactivationDate);
        workPeriodRepository.save(spell);
        payslips.refresh(employee, YearMonth.from(reactivationDate), YearMonth.now());
    }

    /** Allowed date range for the employee's next status change (forward-only, never in the future). */
    @Transactional(readOnly = true)
    public StatusWindowResponse statusWindow(UUID employeeId) {
        return statusWindow(employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee not found")));
    }

    // Next change must come strictly after the last status change (joining / reactivation / inactive
    // date) and after the newest fully paid (locked) month.
    private StatusWindowResponse statusWindow(Employee employee) {
        UUID id = employee.getId();
        Optional<EmployeeWorkPeriod> open = workPeriodRepository.findByEmployeeIdAndEndDateIsNull(id);
        LocalDate lastChange = open.map(EmployeeWorkPeriod::getStartDate).orElseGet(() ->
                workPeriodRepository.findTopByEmployeeIdAndEndDateIsNotNullOrderByEndDateDesc(id)
                        .map(EmployeeWorkPeriod::getEndDate).orElse(employee.getJoiningDate()));
        LocalDate afterChange = lastChange == null ? null : lastChange.plusDays(1);
        return StatusWindowResponse.builder()
                .action(open.isPresent() ? "REACTIVATE" : "INACTIVATE")
                .minDate(later(afterChange, payslips.firstUnlockedDate(id)))
                .maxDate(LocalDate.now()).build();
    }

    private void validateStatusDate(StatusWindowResponse window, LocalDate date, String label) {
        if (date.isAfter(window.getMaxDate())) throw new BusinessException(label + " cannot be in the future");
        if (window.getMinDate() != null && date.isBefore(window.getMinDate())) {
            throw new BusinessException(label + " must be on or after " + window.getMinDate().format(DAY)
                    + " (status changes are forward-only and paid months are locked)");
        }
    }

    private LocalDate later(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
    // Employee list with earnings; monthly salary totals come from a fixed number of batched queries.
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAll() {
        YearMonth ym = YearMonth.now();
        List<Employee> employees = employeeRepository.findAll();
        Map<UUID, BigDecimal> totalByEmp = toEarningMap(productionEntryDetailRepository.sumEarningGroupedByEmployee());
        Map<UUID, BigDecimal> monthByEmp = toEarningMap(productionEntryDetailRepository.sumEarningGroupedByEmployeeBetween(ym.atDay(1), ym.atEndOfMonth()));
        Map<UUID, BigDecimal> givenByEmp = employeeLedgerService.moneyGivenGrouped();
        Map<UUID, PayslipGenerationService.SalaryTotals> salary =
                payslips.totalsFor(employees.stream().filter(this::isMonthly).toList());
        return employees.stream().map(e -> {
            PayslipGenerationService.SalaryTotals s = salary.get(e.getId());
            BigDecimal month = s != null ? s.currentMonth() : monthByEmp.getOrDefault(e.getId(), BigDecimal.ZERO);
            BigDecimal accrued = s != null ? s.totalEarned() : totalByEmp.getOrDefault(e.getId(), BigDecimal.ZERO);
            return toResponse(e, computeEarnings(e, month, accrued, givenByEmp.getOrDefault(e.getId(), BigDecimal.ZERO)));
        }).toList();
    }

    private boolean isMonthly(Employee employee) {
        return employee.getPaymentType() == PaymentType.MONTHLY;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<UUID, BigDecimal> toEarningMap(List<Object[]> rows) {
        Map<UUID, BigDecimal> m = new HashMap<>();
        for (Object[] r : rows) {
            m.put((UUID) r[0], r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1]);
        }
        return m;
    }

    // Single-employee earnings: monthly → salary; piece-rate → piece sums.
    private EmployeeResponse map(Employee employee) {
        BigDecimal month;
        BigDecimal accrued;
        if (isMonthly(employee)) {
            PayslipGenerationService.SalaryTotals totals = payslips.totals(employee);
            month = totals.currentMonth();
            accrued = totals.totalEarned();
        } else {
            YearMonth ym = YearMonth.now();
            month = productionEntryDetailRepository.sumEarningByEmployeeBetween(employee.getId(), ym.atDay(1), ym.atEndOfMonth());
            accrued = productionEntryDetailRepository.sumEarningByEmployee(employee.getId());
        }
        return toResponse(employee, computeEarnings(employee, month, accrued, employeeLedgerService.moneyGivenForEmployee(employee)));
    }

    private record Earnings(BigDecimal currentMonth, BigDecimal overall, BigDecimal pending, BigDecimal advanceBalance) {}

    // Overall earning = money given; pending = earned − given (0 floor); balance from the employee column.
    private Earnings computeEarnings(Employee employee, BigDecimal currentMonth, BigDecimal accrued, BigDecimal moneyGiven) {
        BigDecimal pending = nz(accrued).subtract(nz(moneyGiven)).max(BigDecimal.ZERO);
        return new Earnings(currentMonth, nz(moneyGiven), pending, nz(employee.getAdvanceBalance()));
    }

    private EmployeeResponse toResponse(Employee employee, Earnings earnings) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .mobileNumber(employee.getMobileNumber())
                .email(employee.getEmail())
                .address(employee.getAddress())
                .joiningDate(employee.getJoiningDate())
                .active(employee.getActive())
                .paymentType(employee.getPaymentType())
                .monthlySalary(employee.getMonthlySalary())
                .currentMonthEarning(earnings.currentMonth())
                .totalEarning(earnings.overall())
                .advanceBalance(earnings.advanceBalance())
                .pending(earnings.pending())
                .build();
    }


    private String generateEmployeeCode() {
        long count = employeeRepository.count() + 1;
        return String.format("EMP%04d", count);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new BusinessException("Employee not found"));
        return map(employee);
    }

    public EmployeeResponse update(UUID id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new BusinessException("Employee not found"));
        employee.setFullName(request.getFullName());
        employee.setMobileNumber(request.getMobileNumber());
        employee.setEmail(request.getEmail());
        employee.setAddress(request.getAddress());
        LocalDate joining = request.getJoiningDate();
        if (joining != null && !joining.equals(employee.getJoiningDate())) {
            // Saved months depend on the joining date: drop them so they regenerate (refused once paid).
            payslips.resetSavedMonths(employee.getId());
            employee.setJoiningDate(joining);
        }
        // A status change from the edit form reconciles the inactive spells (effective today).
        applyActiveChange(employee, request.getActive(), null);
        if (request.getPaymentType() != null) employee.setPaymentType(request.getPaymentType());
        employee.setMonthlySalary(request.getMonthlySalary());
        employee = employeeRepository.save(employee);
        return map(employee);
    }

    @Transactional
    public void updateStatus(UUID id, Boolean active, LocalDate effectiveDate) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee not found"));

        if (active == null) {
            throw new BusinessException("Employee status is required.");
        }

        applyActiveChange(employee, active, effectiveDate);
        employeeRepository.save(employee);
    }


    // ── Attendance (exception-based: only absent / half-day days are stored) ──
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendance(UUID employeeId, LocalDate from, LocalDate to) {

        Map<LocalDate, AttendanceStatus> attendanceMap = employeeAttendanceRepository.findByEmployeeIdAndDateBetween(employeeId, from, to).stream().collect(Collectors.toMap(EmployeeAttendance::getDate, EmployeeAttendance::getStatus, (a, b) -> b));
        List<EmployeeWorkPeriod> workPeriods = workPeriodRepository.findOverlappingPeriods(employeeId, from, to);
        List<AttendanceResponse> response = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            boolean inactive = false;
            for (EmployeeWorkPeriod wp : workPeriods) {
                if (!date.isBefore(wp.getStartDate()) && (wp.getEndDate() == null || date.isBefore(wp.getEndDate()))) {
                    inactive = true;
                    break;
                }
            }
            AttendanceStatus attendanceStatus = attendanceMap.get(date);
            if (inactive) {
                response.add(AttendanceResponse.builder().date(date).status(AttendanceStatus.INACTIVE).build());
            } else if (attendanceStatus != null) {
                response.add(AttendanceResponse.builder().date(date).status(attendanceStatus).build());
            }
        }
        return response;
    }


    // Leave in a fully paid (locked) month is rejected; saved months are refreshed after the change.
    public AttendanceResponse markAttendance(UUID employeeId, MarkAttendanceRequest request) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("Employee not found"));
        LocalDate date = request.getDate();
        boolean isInactive = workPeriodRepository.existsInactivePeriodOverlap(employeeId, date,date);
        if (isInactive) {
            throw new RuntimeException("Leave cannot be applied during the employee's inactive period.");
        }
        payslips.assertMonthsOpen(employeeId, date, date);
        EmployeeAttendance att = employeeAttendanceRepository.findByEmployeeIdAndDate(employeeId, date)
                .orElseGet(EmployeeAttendance::new);
        att.setEmployee(employee);
        att.setDate(date);
        att.setStatus(request.getStatus());
        att = employeeAttendanceRepository.save(att);
        payslips.refresh(employee, YearMonth.from(date), YearMonth.from(date));
        return AttendanceResponse.builder().date(att.getDate()).status(att.getStatus()).build();
    }

    public void clearAttendance(UUID employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("Employee not found"));
        payslips.assertMonthsOpen(employeeId, date, date);
        employeeAttendanceRepository.findByEmployeeIdAndDate(employeeId, date)
                .ifPresent(employeeAttendanceRepository::delete);
        payslips.refresh(employee, YearMonth.from(date), YearMonth.from(date));
    }

    // Mark every day in an inclusive range with the same status (e.g. 10th–20th leave) in one batch.
    public List<AttendanceResponse> markAttendanceRange(UUID employeeId, MarkAttendanceRangeRequest request) {
        LocalDate from = request.getFromDate(), to = request.getToDate();
        if (from.isAfter(to)) throw new BusinessException("From date cannot be after To date");
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("Employee not found"));
        boolean isInactive = workPeriodRepository.existsInactivePeriodOverlap(employeeId, from,to);

        if (isInactive) {
            throw new RuntimeException("Leave cannot be applied during the employee's inactive period.");
        }
        payslips.assertMonthsOpen(employeeId, from, to);
        Map<LocalDate, EmployeeAttendance> existing = employeeAttendanceRepository
                .findByEmployeeIdAndDateBetween(employeeId, from, to).stream()
                .collect(Collectors.toMap(EmployeeAttendance::getDate, a -> a));
        List<EmployeeAttendance> rows = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            EmployeeAttendance att = existing.getOrDefault(d, new EmployeeAttendance());
            att.setEmployee(employee);
            att.setDate(d);
            att.setStatus(request.getStatus());
            rows.add(att);
        }
        employeeAttendanceRepository.saveAll(rows);
        payslips.refresh(employee, YearMonth.from(from), YearMonth.from(to));
        return rows.stream().map(a -> AttendanceResponse.builder().date(a.getDate()).status(a.getStatus()).build()).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDetailsResponse getDetails(UUID employeeId, LocalDate fromDate, LocalDate toDate) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("Employee not found"));
        List<ProductionEntry> entries = productionEntryRepository.findByEmployeeWithDetails(employeeId, fromDate, toDate);

        int totalQty = 0;
        BigDecimal totalEarnings = BigDecimal.ZERO;
        // Group piece-work by the WORK/task (product no longer applies); productId stays null.
        Map<String, EmployeeProductSummaryResponse> workMap = new LinkedHashMap<>();
        Map<String, EmployeeCurrentMonthProductResponse> currentMonthMap = new LinkedHashMap<>();
        List<EmployeeProductionHistoryResponse> history = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        BigDecimal currentMonthEarnings = BigDecimal.ZERO;

        for (ProductionEntry entry : entries) {
            boolean isCurrent = YearMonth.from(entry.getProductionDate()).equals(currentMonth);
            for (ProductionEntryDetail detail : entry.getDetails()) {
                BigDecimal earning = detail.getAmountSnapshot() == null ? BigDecimal.ZERO : detail.getAmountSnapshot();
                BigDecimal rate = detail.getRateSnapshot() == null ? BigDecimal.ZERO : detail.getRateSnapshot();
                String work = workLabel(detail);
                totalQty += detail.getQuantity();
                totalEarnings = totalEarnings.add(earning);
                if (isCurrent) currentMonthEarnings = currentMonthEarnings.add(earning);

                EmployeeProductSummaryResponse existing = workMap.get(work);
                if (existing == null) {
                    workMap.put(work, EmployeeProductSummaryResponse.builder().productName(work).quantity(detail.getQuantity()).earnings(earning).build());
                } else {
                    existing.setQuantity(existing.getQuantity() + detail.getQuantity());
                    existing.setEarnings(existing.getEarnings().add(earning));
                }

                history.add(EmployeeProductionHistoryResponse.builder().productionDate(entry.getProductionDate()).productName(work).quantity(detail.getQuantity()).rate(rate).earnings(earning).build());

                if (isCurrent) {
                    EmployeeCurrentMonthProductResponse c = currentMonthMap.get(work);
                    if (c == null) {
                        currentMonthMap.put(work, EmployeeCurrentMonthProductResponse.builder().productName(work).quantity(detail.getQuantity()).earnings(earning).build());
                    } else {
                        c.setQuantity(c.getQuantity() + detail.getQuantity());
                        c.setEarnings(c.getEarnings().add(earning));
                    }
                }
            }
        }

        // Latest production date first (used by both the table and the employee report).
        history.sort(Comparator.comparing(EmployeeProductionHistoryResponse::getProductionDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        YearMonth ym = YearMonth.now();
        double leaveDays = employeeAttendanceRepository
                .findByEmployeeIdAndDateBetween(employeeId, ym.atDay(1), ym.atEndOfMonth())
                .stream().mapToDouble(a -> a.getStatus() == com.skr.erp.common.constants.AttendanceStatus.HALF_DAY ? 0.5 : 1.0).sum();

        // Monthly employees earn salary (saved months + live current month), not piece amounts.
        PayslipGenerationService.SalaryTotals salary = isMonthly(employee) ? payslips.totals(employee) : null;
        BigDecimal totalEarned = salary != null ? salary.totalEarned() : totalEarnings;
        BigDecimal currentEarned = salary != null ? salary.currentMonth() : currentMonthEarnings;

        EmployeeSummaryResponse summary = EmployeeSummaryResponse.builder()
                .totalProductionQty(totalQty)
                .totalEarnings(totalEarned)
                .currentMonthEarnings(currentEarned)
                .productsWorkedOn(workMap.size())
                .currentMonthLeaveDays(leaveDays)
                .build();

        return EmployeeDetailsResponse.builder()
                .employee(map(employee)).summary(summary)
                .productSummary(new ArrayList<>(workMap.values()))
                .currentMonthProductSummary(new ArrayList<>(currentMonthMap.values()))
                .productionHistory(history)
                .build();

    }

    // Label a piece-work line by its task; fall back to any legacy product name.
    private String workLabel(ProductionEntryDetail d) {
        if (d.getWork() != null && !d.getWork().isBlank()) return d.getWork();
        return d.getProductNameSnapshot() != null ? d.getProductNameSnapshot() : "—";
    }

    @Transactional(readOnly = true)
    public EmployeeStatsResponse getStats() {
        List<Employee> employees = employeeRepository.findAll();
        Long totalEmployees = (long) employees.size();
        Long activeEmployees = employees.stream().filter(Employee::getActive).count();
        Long inactiveEmployees = totalEmployees - activeEmployees;
        YearMonth currentMonth = YearMonth.now();
        BigDecimal currentMonthTotal = productionEntryDetailRepository.sumAllEarningBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        // Overall payroll = all money given to employees (advances + expenses + payments).
        BigDecimal overallTotal = employeeLedgerService.totalMoneyGivenAll();

        return EmployeeStatsResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .inactiveEmployees(inactiveEmployees)
                .currentMonthTotalEarning(currentMonthTotal)
                .overallTotalEarning(overallTotal)
                .build();

    }

    @Transactional(readOnly = true)
    public List<EmployeeCalendarResponse> getCalendarData(
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate) {

        // Scoped DB query (employee + date range) instead of loading every production entry into memory.
        List<ProductionEntry> entries =
                productionEntryRepository.findByEmployeeWithDetails(employeeId, fromDate, toDate);

        return entries.stream()

                .collect(
                        Collectors.groupingBy(
                                ProductionEntry::getProductionDate))

                .entrySet()

                .stream()

                .map(entry -> {

                    LocalDate date =
                            entry.getKey();

                    int quantity =
                            entry.getValue()
                                    .stream()
                                    .flatMap(production ->
                                            production.getDetails()
                                                    .stream())
                                    .mapToInt(
                                            ProductionEntryDetail::getQuantity)
                                    .sum();

                    BigDecimal earning =
                            entry.getValue()
                                    .stream()
                                    .flatMap(production ->
                                            production.getDetails()
                                                    .stream())
                                    .map(detail ->
                                            detail.getAmountSnapshot() == null
                                                    ? BigDecimal.ZERO
                                                    : detail.getAmountSnapshot())
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add);

                    return EmployeeCalendarResponse
                            .builder()
                            .date(date)
                            .totalQuantity(quantity)
                            .totalEarning(earning)
                            .build();
                })

                .sorted(
                        Comparator.comparing(
                                EmployeeCalendarResponse::getDate))

                .toList();

    }


}