package com.skr.erp.repository;

import com.skr.erp.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    // New portfolio: copy every setting of the source (default) portfolio, with its own company name
    // and empty company address / contact / GSTIN. Native on purpose: crosses portfolios.
    @Modifying
    @Query(value = """
            INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at)
            SELECT gen_random_uuid(), :targetId, setting_key,
                   CASE setting_key WHEN 'COMPANY_NAME' THEN :companyName
                                    WHEN 'COMPANY_ADDRESS' THEN '' WHEN 'COMPANY_CONTACT' THEN '' WHEN 'COMPANY_GSTIN' THEN ''
                                    ELSE setting_value END,
                   description, now(), now()
            FROM system_setting WHERE portfolio_id = :sourceId
            """, nativeQuery = true)
    int copySettings(@Param("sourceId") UUID sourceId, @Param("targetId") UUID targetId, @Param("companyName") String companyName);
}
