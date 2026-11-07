package org.lawaterwatch.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.dto.LeaderboardRow;
import org.lawaterwatch.api.dto.Sort;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.ViolationRepository.ViolationCount;
import org.lawaterwatch.api.repository.WaterSystemRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//Leaderboard sorting with mocked repositories, no Spring context or database
class LeaderboardTest {

    private final WaterSystemRepository waterSystemRepository = mock(WaterSystemRepository.class);
    private final ViolationRepository violationRepository = mock(ViolationRepository.class);
    private final WaterSystemService service = new WaterSystemService(
            waterSystemRepository, mock(DataMetaRepository.class), violationRepository);

    private static WaterSystem system(String pwsid, String name, Integer waterScore, Integer burdenScore,
                                      Integer population, Integer tracts) {
        WaterSystem system = new WaterSystem(pwsid);
        system.setName(name);
        system.setWaterScore(waterScore);
        system.setBurdenScore(burdenScore);
        system.setPopulation(population);
        system.setTractCount(tracts);
        return system;
    }

    private static ViolationCount count(String pwsid, long violations) {
        return new ViolationCount() {
            public String getPwsid() { return pwsid; }
            public long getViolationCount() { return violations; }
        };
    }

    private static List<String> pwsids(List<LeaderboardRow> rows) {
        return rows.stream().map(LeaderboardRow::pwsid).toList();
    }

    @BeforeEach
    void data() {
        //Each system ranks differently on every column so a wrong comparator shows up
        when(waterSystemRepository.findAll()).thenReturn(List.of(
                system("A", "Alpha", 40, 90, 1_000, 3),
                system("B", "Bravo", 80, 20, 50_000, 9),
                system("C", "Charlie", 60, 50, 10_000, 1)));
        when(violationRepository.countGroupedByPwsid()).thenReturn(List.of(
                count("A", 2),
                count("C", 7)));
    }

    @Test
    void noneKeepsRepositoryOrder() {
        assertEquals(List.of("A", "B", "C"), pwsids(service.leaderboard(Sort.NONE)));
    }

    @Test
    void rowCarriesSystemFieldsAndViolationCount() {
        LeaderboardRow alpha = service.leaderboard(Sort.NONE).getFirst();

        assertEquals(new LeaderboardRow("A", "Alpha", 40, 90, 1_000, 3, 2L), alpha);
    }

    @Test
    void systemWithNoViolationsGetsZero() {
        LeaderboardRow bravo = service.leaderboard(Sort.NONE).get(1);

        assertEquals(0L, bravo.violations());
    }

    @Test
    void waterScoreSortsHighestFirst() {
        assertEquals(List.of("B", "C", "A"), pwsids(service.leaderboard(Sort.WATERSCORE)));
    }

    @Test
    void burdenScoreSortsLowestFirst() {
        assertEquals(List.of("B", "C", "A"), pwsids(service.leaderboard(Sort.BURDENSCORE)));
    }

    @Test
    void violationsSortsFewestFirst() {
        //B has no violation rows, so its 0 puts it on top
        assertEquals(List.of("B", "A", "C"), pwsids(service.leaderboard(Sort.VIOLATIONS)));
    }

    @Test
    void tractsSortsFewestFirst() {
        assertEquals(List.of("C", "A", "B"), pwsids(service.leaderboard(Sort.TRACT)));
    }

    @Test
    void tiesBreakByNameAlphabetically() {
        when(waterSystemRepository.findAll()).thenReturn(List.of(
                system("Z", "Zulu", 70, 30, 5_000, 2),
                system("Y", "Yankee", 70, 30, 5_000, 2),
                system("X", "Xray", 70, 30, 5_000, 2)));
        when(violationRepository.countGroupedByPwsid()).thenReturn(List.of());

        for (Sort sort : Sort.values()) {
            if (sort != Sort.NONE) {
                assertEquals(List.of("X", "Y", "Z"), pwsids(service.leaderboard(sort)), sort.name());
            }
        }
    }

    @Test
    void populationSortsLargestFirst() {
        assertEquals(List.of("B", "C", "A"), pwsids(service.leaderboard(Sort.POPULATION)));
    }

    @Test
    void nullScoresSortLastInsteadOfThrowing() {
        //Scores.waterScore/roundScore return null when a system has no usable tract data
        when(waterSystemRepository.findAll()).thenReturn(List.of(
                system("A", "Alpha", null, null, null, null),
                system("B", "Bravo", 80, 20, 50_000, 9)));

        for (Sort sort : List.of(Sort.WATERSCORE, Sort.BURDENSCORE, Sort.POPULATION, Sort.TRACT)) {
            assertEquals(List.of("B", "A"), pwsids(service.leaderboard(sort)), sort.name());
        }
    }

    @Test
    void emptyDatabaseGivesEmptyLeaderboard() {
        when(waterSystemRepository.findAll()).thenReturn(List.of());
        when(violationRepository.countGroupedByPwsid()).thenReturn(List.of());

        for (Sort sort : Sort.values()) {
            assertTrue(service.leaderboard(sort).isEmpty(), sort.name());
        }
    }
}
