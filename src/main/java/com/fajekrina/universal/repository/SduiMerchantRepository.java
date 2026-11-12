package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiMerchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiMerchantRepository extends JpaRepository<SduiMerchant, String> {
    boolean existsByMerchantIdAndStatus(String merchantId, String status);
}
