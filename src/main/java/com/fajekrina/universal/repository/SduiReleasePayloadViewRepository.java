package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiReleasePayloadView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiReleasePayloadViewRepository extends JpaRepository<SduiReleasePayloadView, Long> {
    // read-only: v_sdui_release_payload
}
