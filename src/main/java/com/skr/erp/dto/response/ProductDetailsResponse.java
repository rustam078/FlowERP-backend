package com.skr.erp.dto.response;

import com.skr.erp.common.constants.ProductSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductDetailsResponse {

    private UUID id;
    private String name;
    private String iconName;
    private String description;
    private ProductSource source;
    private Boolean active;
    private Integer totalPieceCodes;
    private Integer activePieceCodes;
    private Integer inactivePieceCodes;
    private Boolean hasImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductPieceCodeResponse> pieceCodes;
}