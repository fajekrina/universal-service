package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiStyle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiStyleRepository extends JpaRepository<SduiStyle, Long> {
    Optional<SduiStyle> findFirstByMerchantIdAndStyleKeyOrderByVersionDesc(String merchantId, String styleKey);
    
    Optional<SduiStyle> findByMerchantIdAndStyleKeyAndVersion(String merchantId, String styleKey, Integer version);
    
    Optional<SduiStyle> findByMerchantIdAndStyleKeyAndContentHash(String merchantId, String styleKey, String contentHash);
}
