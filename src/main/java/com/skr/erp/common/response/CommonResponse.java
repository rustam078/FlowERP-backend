package com.skr.erp.common.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommonResponse<T> {
    private boolean success;
    private String message;
    private T data;
}