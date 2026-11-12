package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiSigningKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiSigningKeyRepository extends JpaRepository<SduiSigningKey, String> {
    List<SduiSigningKey> findAllByStatusNotOrderByCreatedTime(String status);
}
