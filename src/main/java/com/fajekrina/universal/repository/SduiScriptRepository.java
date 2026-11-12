package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiScript;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiScriptRepository extends JpaRepository<SduiScript, Long> {
    Optional<SduiScript> findByPageIdAndContentHash(String pageId, String contentHash);
    
    Optional<SduiScript> findFirstByPageIdOrderByVersionDesc(String pageId);
}
