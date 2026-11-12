package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiScreenRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SduiScreenReleaseRepository extends JpaRepository<SduiScreenRelease, Long> {

    Optional<SduiScreenRelease> findFirstByMerchantIdAndScreenKeyOrderBySequenceDesc(String merchantId, String screenKey);

    /** Highest release id of a merchant: the manifest sequence (monotonic, never reused). */
    @Query("select max(r.releaseId) from SduiScreenRelease r where r.merchantId = :merchantId")
    Optional<Long> maxReleaseId(@Param("merchantId") String merchantId);

    List<SduiScreenRelease> findAllByReleaseIdIn(List<Long> releaseIds);
}
