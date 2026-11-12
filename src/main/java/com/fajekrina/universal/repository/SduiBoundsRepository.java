package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiBounds;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiBoundsRepository extends JpaRepository<SduiBounds, Integer> {
    Optional<SduiBounds> findFirstByOrderByBoundsVersionDesc();
}
