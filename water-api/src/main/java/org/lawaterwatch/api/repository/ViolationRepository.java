package org.lawaterwatch.api.repository;

import org.lawaterwatch.api.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    List<Violation> findByPwsid(String pwsid);

    long countByPwsid(String pwsid);

}
