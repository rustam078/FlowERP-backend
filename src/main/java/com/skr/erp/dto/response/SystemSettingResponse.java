package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemSettingResponse {

    private String key;
    private String value;
    private String description;

}