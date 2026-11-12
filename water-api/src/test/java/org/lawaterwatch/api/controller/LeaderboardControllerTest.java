package org.lawaterwatch.api.controller;

import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.dto.LeaderboardRow;
import org.lawaterwatch.api.dto.Sort;
import org.lawaterwatch.api.service.MapResponseCache;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Request param binding and JSON shape for /api/systems/leaderboard
@WebMvcTest(WaterController.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaterSystemService waterSystemService;

    @MockitoBean
    private MapResponseCache mapResponseCache;

    @Test
    void missingSortDefaultsToNone() throws Exception {
        when(waterSystemService.leaderboard(Sort.NONE)).thenReturn(List.of(
                new LeaderboardRow("CA0001", "Alpha", 40, 90, 1_000, 3, 2L)));

        mockMvc.perform(get("/api/systems/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pwsid").value("CA0001"))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[0].waterScore").value(40))
                .andExpect(jsonPath("$[0].burdenScore").value(90))
                .andExpect(jsonPath("$[0].population").value(1_000))
                .andExpect(jsonPath("$[0].tracts").value(3))
                .andExpect(jsonPath("$[0].violations").value(2));

        verify(waterSystemService).leaderboard(Sort.NONE);
    }

    @Test
    void sortParamIsPassedThrough() throws Exception {
        when(waterSystemService.leaderboard(Sort.VIOLATIONS)).thenReturn(List.of());

        mockMvc.perform(get("/api/systems/leaderboard").param("sort", "VIOLATIONS"))
                .andExpect(status().isOk());

        verify(waterSystemService).leaderboard(Sort.VIOLATIONS);
    }

    @Test
    void unknownSortIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/systems/leaderboard").param("sort", "NOPE"))
                .andExpect(status().isBadRequest());
    }
}
