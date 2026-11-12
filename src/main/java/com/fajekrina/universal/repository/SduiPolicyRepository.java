package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiPolicy;
import com.fajekrina.universal.model.entity.SduiPolicyId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiPolicyRepository extends JpaRepository<SduiPolicy, SduiPolicyId> {
    Optional<SduiPolicy> findFirstByMerchantIdOrderByPolicyVersionDesc(String merchantId);
    
    Optional<SduiPolicy> findByMerchantIdAndPolicyVersion(String merchantId, Integer policyVersion);
}
