package com.skr.erp.service;

import com.skr.erp.dto.request.CreateVendorRequest;
import com.skr.erp.dto.request.UpdateVendorRequest;
import com.skr.erp.dto.response.VendorDetailsResponse;
import com.skr.erp.dto.response.VendorPurchaseItemResponse;
import com.skr.erp.dto.response.VendorPurchaseResponse;
import com.skr.erp.dto.response.VendorResponse;
import com.skr.erp.entity.Investment;
import com.skr.erp.entity.InvestmentItem;
import com.skr.erp.entity.Vendor;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.InvestmentPaymentRepository;
import com.skr.erp.repository.InvestmentRepository;
import com.skr.erp.repository.VendorRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestmentPaymentRepository investmentPaymentRepository;

    @Transactional
    public VendorResponse create(CreateVendorRequest request) {
        validateDuplicate(request.getName(), request.getMobile(), request.getEmail(), request.getGstNumber(), null);

        Vendor vendor = new Vendor();

        vendor.setName(request.getName());
        vendor.setContactName(request.getContactName());
        vendor.setMobile(request.getMobile());
        vendor.setEmail(request.getEmail());
        vendor.setGstNumber(request.getGstNumber());
        vendor.setAddress(request.getAddress());
        vendor.setActive(true);

        vendor = vendorRepository.save(vendor);

        return map(vendor);
    }

    @Transactional
    public VendorResponse update(UUID id, UpdateVendorRequest request) {

        Vendor vendor = vendorRepository.findById(id).orElseThrow(() -> new BusinessException("Vendor not found"));
        validateDuplicate(request.getName(), request.getMobile(), request.getEmail(), request.getGstNumber(), id);

        vendor.setName(request.getName());
        vendor.setContactName(request.getContactName());
        vendor.setMobile(request.getMobile());
        vendor.setEmail(request.getEmail());
        vendor.setGstNumber(request.getGstNumber());
        vendor.setAddress(request.getAddress());
        vendor = vendorRepository.save(vendor);

        return map(vendor);
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> search(String name, String mobile, Boolean active) {

        Specification<Vendor> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }

            if (mobile != null && !mobile.isBlank()) {
                predicates.add(cb.like(root.get("mobile"), "%" + mobile + "%"));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return vendorRepository.findAll(specification).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public VendorDetailsResponse getById(UUID id) {

        Vendor vendor = vendorRepository.findById(id).orElseThrow(() -> new BusinessException("Vendor not found"));
        List<Investment> investments = investmentRepository.findAllWithItemsByVendorId(id);

        Integer totalInvoices = investments.size();
        BigDecimal totalPurchaseAmount = investments.stream()
                .map(Investment::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate lastPurchaseDate = investments.isEmpty() ? null : investments.getFirst().getPurchaseDate();

        return VendorDetailsResponse.builder()
                .id(vendor.getId()).name(vendor.getName())
                .mobile(vendor.getMobile()).email(vendor.getEmail())
                .address(vendor.getAddress()).active(vendor.getActive())
                .totalInvoices(totalInvoices)
                .totalPurchaseAmount(totalPurchaseAmount)
                .lastPurchaseDate(lastPurchaseDate)
                .purchases(investments.stream().map(this::toVendorPurchase).toList())
                .build();
    }

    @Transactional
    public VendorResponse toggleStatus(UUID id) {

        Vendor vendor = vendorRepository.findById(id).orElseThrow(() -> new BusinessException("Vendor not found"));
        vendor.setActive(!vendor.getActive());
        vendor = vendorRepository.save(vendor);
        return map(vendor);
    }


    private void validateDuplicate(String name, String mobile, String email, String gst, UUID currentId) {

        vendorRepository.findByNameIgnoreCase(name).ifPresent(v -> {
            if (!v.getId().equals(currentId)) {
                throw new BusinessException("Vendor name already exists");
            }
        });

        vendorRepository.findByMobile(mobile).ifPresent(v -> {
            if (!v.getId().equals(currentId)) {
                throw new BusinessException("Mobile already exists");
            }
        });

        if (email != null && !email.isBlank()) {
            vendorRepository.findByEmailIgnoreCase(email).ifPresent(v -> {
                if (!v.getId().equals(currentId)) {
                    throw new BusinessException("Email already exists");
                }
            });
        }

        if (gst != null && !gst.isBlank()) {
            vendorRepository.findByGstNumberIgnoreCase(gst).ifPresent(v -> {
                if (!v.getId().equals(currentId)) {
                    throw new BusinessException("GST already exists");
                }
            });
        }
    }

    private VendorResponse map(Vendor vendor) {
        return VendorResponse.builder()
                .id(vendor.getId()).name(vendor.getName())
                .contactName(vendor.getContactName())
                .mobile(vendor.getMobile()).email(vendor.getEmail())
                .gstNumber(vendor.getGstNumber()).address(vendor.getAddress())
                .active(vendor.getActive()).createdAt(vendor.getCreatedAt())
                .updatedAt(vendor.getUpdatedAt()).build();
    }

    private VendorPurchaseResponse toVendorPurchase(Investment investment) {

        BigDecimal grand = investment.getGrandTotal() != null ? investment.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal paid = investmentPaymentRepository.sumByInvestment(investment.getId());
        if (paid == null) paid = BigDecimal.ZERO;
        BigDecimal due = grand.subtract(paid).max(BigDecimal.ZERO);
        String status = paid.compareTo(BigDecimal.ZERO) <= 0 ? "PENDING" : (paid.compareTo(grand) >= 0 ? "PAID" : "PARTIALLY_PAID");

        return VendorPurchaseResponse.builder()
                .invoiceNumber(investment.getInvoiceNumber())
                .investmentType(investment.getInvestmentType())
                .purchaseDate(investment.getPurchaseDate())
                .grandTotal(investment.getGrandTotal())
                .amountPaid(paid).amountDue(due).paymentStatus(status)
                .items(investment.getItems().stream().map(this::toVendorPurchaseItem).toList())
                .build();
    }

    private VendorPurchaseItemResponse toVendorPurchaseItem(InvestmentItem item) {
        return VendorPurchaseItemResponse.builder()
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .itemType(item.getItemType())
                .itemName(item.getItemName())
                .quantity(item.getQuantity()).
                unit(item.getUnit()).rate(item.getRate())
                .totalAmount(item.getTotalAmount())
                .build();
    }
}