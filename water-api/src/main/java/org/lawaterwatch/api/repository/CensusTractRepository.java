package org.lawaterwatch.api.repository;

import org.lawaterwatch.api.entity.CensusTract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CensusTractRepository extends JpaRepository<CensusTract, String> {
}
