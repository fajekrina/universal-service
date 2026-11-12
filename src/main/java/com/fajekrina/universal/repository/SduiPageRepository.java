package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.enums.PageStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiPageRepository extends JpaRepository<SduiPage, String> {
    Optional<SduiPage> findByMerchantIdAndPageKey(String merchantId, String pageKey);
    
    List<SduiPage> findAllByMerchantIdAndStatusNotOrderByPageKey(String merchantId, PageStatus status);
    
    /** Serializes concurrent publishes of one page. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SduiPage p where p.pageId = :pageId")
    Optional<SduiPage> findForUpdate(@Param("pageId") String pageId);
}
