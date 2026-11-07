package org.lawaterwatch.api.repository;

import org.lawaterwatch.api.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    List<Violation> findByPwsid(String pwsid);

    long countByPwsid(String pwsid);

    //One row per water system, systems with no violations are absent from the result
    @Query("select v.pwsid as pwsid, count(v) as violationCount from Violation v group by v.pwsid")
    List<ViolationCount> countGroupedByPwsid();

    interface ViolationCount {
        String getPwsid();

        long getViolationCount();
    }

}
