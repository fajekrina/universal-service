package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiScreen;
import com.fajekrina.universal.model.entity.SduiScreenId;
import com.fajekrina.universal.model.enums.PageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SduiScreenRepository extends JpaRepository<SduiScreen, SduiScreenId> {

    Optional<SduiScreen> findByMerchantIdAndScreenKey(String merchantId, String screenKey);

    List<SduiScreen> findAllByMerchantIdAndStatusNotOrderByScreenKey(String merchantId, PageStatus status);

    /** Serializes concurrent publishes of one screen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SduiScreen s where s.merchantId = :merchantId and s.screenKey = :screenKey")
    Optional<SduiScreen> findForUpdate(@Param("merchantId") String merchantId, @Param("screenKey") String screenKey);
}
