package com.skr.erp.service;

import com.skr.erp.dto.response.SystemSettingResponse;
import com.skr.erp.entity.SystemSetting;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingService {

    private final SystemSettingRepository repository;

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAll() {
        return repository.findAll().stream()
                .map(s -> SystemSettingResponse.builder()
                        .key(s.getSettingKey())
                        .value(s.getSettingValue())
                        .description(s.getDescription())
                        .build()).toList();
    }

    @Transactional(readOnly = true)
    public SystemSettingResponse get(String key) {

        SystemSetting setting = repository.findBySettingKey(key).orElseThrow(() -> new BusinessException("Setting not found."));
        return SystemSettingResponse.builder()
                .key(setting.getSettingKey())
                .value(setting.getSettingValue())
                .description(setting.getDescription())
                .build();
    }

    public SystemSettingResponse update(String key, String value) {
        SystemSetting setting = repository.findBySettingKey(key).orElseThrow(() -> new BusinessException("Setting not found."));
        setting.setSettingValue(value);
        repository.save(setting);

        return SystemSettingResponse.builder()
                .key(setting.getSettingKey())
                .value(setting.getSettingValue())
                .description(setting.getDescription()).build();
    }

    @Transactional(readOnly = true)
    public Integer getLowStockThreshold() {
        return repository.findBySettingKey("LOW_STOCK_THRESHOLD")
                .map(s -> Integer.parseInt(s.getSettingValue())).orElse(50);
    }
}