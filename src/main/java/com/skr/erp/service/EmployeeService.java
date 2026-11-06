package com.skr.erp.service;

import com.skr.erp.dto.request.CreateEmployeeRequest;
import com.skr.erp.dto.request.UpdateEmployeeRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.entity.Employee;
import com.skr.erp.entity.ProductRate;
import com.skr.erp.entity.ProductionEntry;
import com.skr.erp.entity.ProductionEntryDetail;
import com.skr.erp.exception.BusinessException;
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
    private final ProductionEntryRepository productionEntryRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final ProductRateRepository productRateRepository;

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
        employee = employeeRepository.save(employee);
        return map(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAll() {
        YearMonth ym = YearMonth.now();
        Map<UUID, BigDecimal> totalByEmp = toEarningMap(productionEntryDetailRepository.sumEarningGroupedByEmployee());
        Map<UUID, BigDecimal> monthByEmp = toEarningMap(productionEntryDetailRepository.sumEarningGroupedByEmployeeBetween(ym.atDay(1), ym.atEndOfMonth()));
        return employeeRepository.findAll().stream()
                .map(e -> toResponse(e,
                        monthByEmp.getOrDefault(e.getId(), BigDecimal.ZERO),
                        totalByEmp.getOrDefault(e.getId(), BigDecimal.ZERO)))
                .toList();
    }

    private Map<UUID, BigDecimal> toEarningMap(List<Object[]> rows) {
        Map<UUID, BigDecimal> m = new HashMap<>();
        for (Object[] r : rows) {
            m.put((UUID) r[0], r[1] == null ? BigDecimal.ZERO : (BigDecimal) r[1]);
        }
        return m;
    }

    // Single-employee earnings via indexed SUM queries (no full-table scan).
    private EmployeeResponse map(Employee employee) {
        YearMonth ym = YearMonth.now();
        BigDecimal month = productionEntryDetailRepository.sumEarningByEmployeeBetween(employee.getId(), ym.atDay(1), ym.atEndOfMonth());
        BigDecimal total = productionEntryDetailRepository.sumEarningByEmployee(employee.getId());
        return toResponse(employee, month, total);
    }

    private EmployeeResponse toResponse(Employee employee, BigDecimal currentMonthEarning, BigDecimal totalEarning) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .mobileNumber(employee.getMobileNumber())
                .email(employee.getEmail())
                .address(employee.getAddress())
                .joiningDate(employee.getJoiningDate())
                .active(employee.getActive())
                .currentMonthEarning(currentMonthEarning)
                .totalEarning(totalEarning)
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
        employee = employeeRepository.save(employee);
        return map(employee);
    }

    public void updateStatus(UUID id, Boolean active) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new BusinessException("Employee not found"));
        employee.setActive(active);
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeDetailsResponse getDetails(UUID employeeId, LocalDate fromDate, LocalDate toDate) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("Employee not found"));
        List<ProductionEntry> entries = productionEntryRepository.findByEmployeeWithDetails(employeeId, fromDate, toDate);

        int totalQty = 0;
        BigDecimal totalEarnings = BigDecimal.ZERO;
        Map<UUID, EmployeeProductSummaryResponse> productMap = new HashMap<>();
        List<EmployeeProductionHistoryResponse> history = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        BigDecimal currentMonthEarnings = BigDecimal.ZERO;

        for (ProductionEntry entry : entries) {
            for (ProductionEntryDetail detail : entry.getDetails()) {
                BigDecimal earning = detail.getAmountSnapshot() == null ? BigDecimal.ZERO : detail.getAmountSnapshot();
                BigDecimal rate = detail.getRateSnapshot() == null ? BigDecimal.ZERO : detail.getRateSnapshot();
                totalQty += detail.getQuantity();
                totalEarnings = totalEarnings.add(earning);
                if (YearMonth.from(entry.getProductionDate()).equals(currentMonth)) {
                    currentMonthEarnings = currentMonthEarnings.add(earning);
                }
                UUID productId = detail.getProduct().getId();
                EmployeeProductSummaryResponse existing = productMap.get(productId);
                if (existing == null) {
                    productMap.put(productId, EmployeeProductSummaryResponse.builder().productId(productId).productName(detail.getProductNameSnapshot()).quantity(detail.getQuantity()).earnings(earning).build());
                } else {
                    existing.setQuantity(existing.getQuantity() + detail.getQuantity());
                    existing.setEarnings(existing.getEarnings().add(earning));
                }
                history.add(EmployeeProductionHistoryResponse.builder().productionDate(entry.getProductionDate()).productId(productId).productName(detail.getProductNameSnapshot()).quantity(detail.getQuantity()).rate(rate).earnings(earning).build());
            }
        }

        // Latest production date first (used by both the table and the employee report).
        history.sort(Comparator.comparing(EmployeeProductionHistoryResponse::getProductionDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<EmployeeCurrentMonthProductResponse> currentMonthProductSummary = entries.stream()
                        .filter(entry -> YearMonth.from(entry.getProductionDate()).equals(currentMonth))
                        .flatMap(entry -> entry.getDetails().stream())
                        .collect(Collectors.groupingBy(detail -> detail.getProduct().getId()))
                        .values()
                        .stream()
                        .map(details -> {
                            ProductionEntryDetail first = details.get(0);
                            int quantity = details.stream().mapToInt(ProductionEntryDetail::getQuantity).sum();
                            BigDecimal earnings = details.stream().map(detail -> detail.getAmountSnapshot() == null ? BigDecimal.ZERO : detail.getAmountSnapshot()).reduce(BigDecimal.ZERO, BigDecimal::add);
                            return EmployeeCurrentMonthProductResponse.builder().productId(first.getProduct().getId()).productName(first.getProductNameSnapshot()).quantity(quantity).earnings(earnings).build();
                        })
                        .toList();

        EmployeeSummaryResponse summary = EmployeeSummaryResponse.builder()
                .totalProductionQty(totalQty)
                .totalEarnings(totalEarnings)
                .currentMonthEarnings(currentMonthEarnings)
                .productsWorkedOn(productMap.size())
                .build();

        return EmployeeDetailsResponse.builder()
                .employee(map(employee)).summary(summary)
                .productSummary(new ArrayList<>(productMap.values()))
                .currentMonthProductSummary(currentMonthProductSummary)
                .productionHistory(history)
                .build();

    }

    @Transactional(readOnly = true)
    public EmployeeStatsResponse getStats() {
        List<Employee> employees = employeeRepository.findAll();
        Long totalEmployees = (long) employees.size();
        Long activeEmployees = employees.stream().filter(Employee::getActive).count();
        Long inactiveEmployees = totalEmployees - activeEmployees;
        YearMonth currentMonth = YearMonth.now();
        BigDecimal currentMonthTotal = productionEntryDetailRepository.sumAllEarningBetween(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        BigDecimal overallTotal = productionEntryDetailRepository.sumAllEarning();

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