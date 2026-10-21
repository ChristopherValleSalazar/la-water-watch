package org.lawaterwatch.api.repository;

import org.lawaterwatch.api.entity.WaterSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaterSystemRepository extends JpaRepository<WaterSystem, String> {
}
