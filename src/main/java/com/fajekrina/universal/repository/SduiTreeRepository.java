package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiTree;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiTreeRepository extends JpaRepository<SduiTree, Long> {
    Optional<SduiTree> findByPageIdAndContentHash(String pageId, String contentHash);
    
    Optional<SduiTree> findByPageIdAndVersion(String pageId, Integer version);
    
    Optional<SduiTree> findFirstByPageIdOrderByVersionDesc(String pageId);
    
    List<SduiTree> findAllByPageIdOrderByVersionDesc(String pageId);
}
