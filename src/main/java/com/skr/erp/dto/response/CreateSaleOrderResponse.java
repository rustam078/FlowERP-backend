package com.skr.erp.dto.response;


import com.skr.erp.common.constants.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleOrderResponse {

    private UUID saleOrderId;
    private String orderNumber;
    private String invoiceNumber;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private PaymentStatus paymentStatus;
}