package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiRelease;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiReleaseRepository extends JpaRepository<SduiRelease, Long> {
    Optional<SduiRelease> findFirstByPageIdOrderBySequenceDesc(String pageId);
    
    Optional<SduiRelease> findByPageIdAndSequence(String pageId, Integer sequence);
    
    List<SduiRelease> findAllByPageIdOrderBySequenceDesc(String pageId);
}
