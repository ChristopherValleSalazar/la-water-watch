package org.lawaterwatch.api.dto;

public record LeaderboardRow(
        String pwsid,
        String name,
        Integer waterScore,
        Integer burdenScore,
        Integer population,
        Integer tracts,
        Long violations
) {
}
