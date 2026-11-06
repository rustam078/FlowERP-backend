package com.skr.erp.dto.response;

import com.skr.erp.common.constants.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AttendanceResponse {
    private LocalDate date;
    private AttendanceStatus status;
}
