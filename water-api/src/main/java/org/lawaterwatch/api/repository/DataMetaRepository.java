package org.lawaterwatch.api.repository;

import org.lawaterwatch.api.entity.DataMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataMetaRepository extends JpaRepository<DataMeta, Integer> {
}
