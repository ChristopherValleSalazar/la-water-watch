package org.lawaterwatch.api.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.entity.Violation;
import org.lawaterwatch.api.repository.ViolationRepository.ViolationCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

//Runs the real grouped JPQL query against its own SQLite file
@SpringBootTest(properties = {
        "app.data.load-on-startup=false",
        "spring.datasource.url=jdbc:sqlite:target/test-violation-repo.db"
})
class ViolationRepositoryTest {

    @Autowired
    private ViolationRepository violationRepository;

    @BeforeEach
    void clear() {
        violationRepository.deleteAll();
    }

    private Map<String, Long> grouped() {
        return violationRepository.countGroupedByPwsid().stream()
                .collect(Collectors.toMap(ViolationCount::getPwsid, ViolationCount::getViolationCount));
    }

    @Test
    void countsRowsPerSystem() {
        //Same EPA id in two systems is two separate violations
        violationRepository.save(new Violation("V1", "CA0001"));
        violationRepository.save(new Violation("V2", "CA0001"));
        violationRepository.save(new Violation("V3", "CA0001"));
        violationRepository.save(new Violation("V1", "CA0002"));

        assertEquals(Map.of("CA0001", 3L, "CA0002", 1L), grouped());
    }

    @Test
    void matchesPerSystemCount() {
        violationRepository.save(new Violation("V1", "CA0001"));
        violationRepository.save(new Violation("V2", "CA0001"));

        assertEquals(violationRepository.countByPwsid("CA0001"), grouped().get("CA0001"));
    }

    @Test
    void emptyTableGivesNoRows() {
        assertTrue(violationRepository.countGroupedByPwsid().isEmpty());
    }
}
