package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiBehavior;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiBehaviorRepository extends JpaRepository<SduiBehavior, String> {
    List<SduiBehavior> findAllByEnabledTrue();
}
