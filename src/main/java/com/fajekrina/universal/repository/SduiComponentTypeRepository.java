package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiComponentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiComponentTypeRepository extends JpaRepository<SduiComponentType, String> {
    List<SduiComponentType> findAllByOrderByComponentType();
}
