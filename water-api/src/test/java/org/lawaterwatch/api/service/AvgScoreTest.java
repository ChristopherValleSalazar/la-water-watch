package org.lawaterwatch.api.service;

import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.dto.AvgScoreResponse;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.WaterSystemRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//Average water and burden scores with a mocked repository, no Spring context or database
class AvgScoreTest {

    private final WaterSystemRepository waterSystemRepository = mock(WaterSystemRepository.class);
    private final WaterSystemService service = new WaterSystemService(
            waterSystemRepository, mock(DataMetaRepository.class), mock(ViolationRepository.class));

    //The divisor is the fixed tracked list, not however many rows came back
    private final double listSize = WaterSystemList.PWSIDS.size();

    private static WaterSystem system(String pwsid, Integer waterScore, Integer burdenScore) {
        WaterSystem system = new WaterSystem(pwsid);
        system.setWaterScore(waterScore);
        system.setBurdenScore(burdenScore);
        return system;
    }

    @Test
    void dividesByFullSystemList() {
        when(waterSystemRepository.findAll()).thenReturn(List.of(
                system("A", 40, 90),
                system("B", 80, 20),
                system("C", 60, 50)));

        AvgScoreResponse avg = service.avgScore();

        assertEquals((int) Math.round(180 / listSize), avg.avgWaterScore());
        assertEquals((int) Math.round(160 / listSize), avg.avgBurdenScore());
    }

    @Test
    void roundsInsteadOfTruncating() {
        //Sum chosen so the quotient lands on 10.5 or just above for the list size
        int size = WaterSystemList.PWSIDS.size();
        int total = size * 10 + (size + 1) / 2;
        when(waterSystemRepository.findAll()).thenReturn(List.of(system("A", total, total)));

        AvgScoreResponse avg = service.avgScore();

        assertEquals(11, avg.avgWaterScore());
        assertEquals(11, avg.avgBurdenScore());
    }

    @Test
    void skipsNullScores() {
        when(waterSystemRepository.findAll()).thenReturn(List.of(
                system("A", null, null),
                system("B", 80, 20)));

        AvgScoreResponse avg = service.avgScore();

        assertEquals((int) Math.round(80 / listSize), avg.avgWaterScore());
        assertEquals((int) Math.round(20 / listSize), avg.avgBurdenScore());
    }

    @Test
    void emptyDatabaseIsZero() {
        when(waterSystemRepository.findAll()).thenReturn(List.of());

        assertEquals(new AvgScoreResponse(0, 0), service.avgScore());
    }
}
