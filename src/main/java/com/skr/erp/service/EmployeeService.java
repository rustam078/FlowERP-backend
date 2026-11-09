package com.skr.erp.service;

import com.skr.erp.dto.request.CreateEmployeeRequest;
import com.skr.erp.dto.request.MarkAttendanceRequest;
import com.skr.erp.dto.request.MarkAttendanceRangeRequest;
import com.skr.erp.dto.request.UpdateEmployeeRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.common.constants.PaymentType;
import com.skr.erp.entity.Employee;
import com.skr.erp.entity.EmployeeAttendance;
import com.skr.erp.entity.ProductRate;
import com.skr.erp.entity.ProductionEntry;
import com.skr.erp.entity.ProductionEntryDetail;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.EmployeeAttendanceRepository;
import com.skr.erp.repository.EmployeeRepository;
import com.skr.erp.repository.ProductRateRepository;
import com.skr.erp.repository.ProductionEntryDetailRepository;
import com.skr.erp.repository.ProductionEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private final EmployeeLedgerService employeeLedgerService;

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

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAll() {
        YearMonth ym = YearMonth.now();
        Map<UUID, BigDecimal> totalByEmp = toEarningMap(productionEntryDetailRepository.sumEarningGroupedByEmployee());
        Map<UUID, BigDecimal> monthByEmp = toEarningMap(productionEntryDetailRepository.sumEarningGroupedByEmployeeBetween(ym.atDay(1), ym.atEndOfMonth()));
        Map<UUID, BigDecimal> givenByEmp = employeeLedgerService.moneyGivenGrouped();
        return employeeRepository.findAll().stream().map(e -> {
            boolean monthly = e.getPaymentType() == PaymentType.MONTHLY;
            BigDecimal month = monthly ? employeeLedgerService.currentMonthSalaryPayable(e) : monthByEmp.getOrDefault(e.getId(), BigDecimal.ZERO);
            BigDecimal accrued = monthly ? employeeLedgerService.totalAccruedSalary(e) : totalByEmp.getOrDefault(e.getId(), BigDecimal.ZERO);
            BigDecimal moneyGiven = givenByEmp.getOrDefault(e.getId(), BigDecimal.ZERO);
            return toResponse(e, computeEarnings(e, month, accrued, moneyGiven));
        }).toList();
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
        boolean monthly = employee.getPaymentType() == PaymentType.MONTHLY;
        YearMonth ym = YearMonth.now();
        BigDecimal month = monthly ? employeeLedgerService.currentMonthSalaryPayable(employee)
                : productionEntryDetailRepository.sumEarningByEmployeeBetween(employee.getId(), ym.atDay(1), ym.atEndOfMonth());
        BigDecimal accrued = monthly ? employeeLedgerService.totalAccruedSalary(employee)
                : productionEntryDetailRepository.sumEarningByEmployee(employee.getId());
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
        employee.setJoiningDate(request.getJoiningDate());
        employee.setActive(request.getActive());
        if (request.getPaymentType() != null) employee.setPaymentType(request.getPaymentType());
        employee.setMonthlySalary(request.getMonthlySalary());
        employee = employeeRepository.save(employee);
        return map(employee);
    }

    public void updateStatus(UUID id, Boolean active) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new BusinessException("Employee not found"));
        employee.setActive(active);
        employeeRepository.save(employee);
    }

    // ── Attendance (exception-based: only absent / half-day days are stored) ──
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendance(UUID employeeId, LocalDate from, LocalDate to) {
        return employeeAttendanceRepository.findByEmployeeIdAndDateBetween(employeeId, from, to).stream()
                .map(a -> AttendanceResponse.builder().date(a.getDate()).status(a.getStatus()).build())
                .toList();
    }

    public AttendanceResponse markAttendance(UUID employeeId, MarkAttendanceRequest request) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("Employee not found"));
        EmployeeAttendance att = employeeAttendanceRepository.findByEmployeeIdAndDate(employeeId, request.getDate())
                .orElseGet(EmployeeAttendance::new);
        att.setEmployee(employee);
        att.setDate(request.getDate());
        att.setStatus(request.getStatus());
        att = employeeAttendanceRepository.save(att);
        return AttendanceResponse.builder().date(att.getDate()).status(att.getStatus()).build();
    }

    public void clearAttendance(UUID employeeId, LocalDate date) {
        employeeAttendanceRepository.findByEmployeeIdAndDate(employeeId, date)
                .ifPresent(employeeAttendanceRepository::delete);
    }

    // Mark every day in an inclusive range with the same status (e.g. 10th–20th leave).
    public List<AttendanceResponse> markAttendanceRange(UUID employeeId, MarkAttendanceRangeRequest request) {
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new BusinessException("From date cannot be after To date");
        }
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("Employee not found"));
        List<AttendanceResponse> result = new ArrayList<>();
        for (LocalDate d = request.getFromDate(); !d.isAfter(request.getToDate()); d = d.plusDays(1)) {
            EmployeeAttendance att = employeeAttendanceRepository.findByEmployeeIdAndDate(employeeId, d)
                    .orElseGet(EmployeeAttendance::new);
            att.setEmployee(employee);
            att.setDate(d);
            att.setStatus(request.getStatus());
            att = employeeAttendanceRepository.save(att);
            result.add(AttendanceResponse.builder().date(att.getDate()).status(att.getStatus()).build());
        }
        return result;
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

        // Monthly employees earn salary (accrued), not piece amounts.
        boolean monthly = employee.getPaymentType() == PaymentType.MONTHLY;
        BigDecimal totalEarned = monthly ? employeeLedgerService.totalAccruedSalary(employee) : totalEarnings;
        BigDecimal currentEarned = monthly ? employeeLedgerService.currentMonthSalaryPayable(employee) : currentMonthEarnings;

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