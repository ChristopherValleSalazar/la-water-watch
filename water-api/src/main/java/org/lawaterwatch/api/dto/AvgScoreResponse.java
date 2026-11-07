package org.lawaterwatch.api.dto;

public record AvgScoreResponse(
        Integer avgWaterScore,
        Integer avgBurdenScore
) {
}
