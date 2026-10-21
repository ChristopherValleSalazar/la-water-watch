package org.lawaterwatch.api.service;

import org.lawaterwatch.api.entity.CensusTract;

import java.util.List;
import java.util.function.Function;

//Population weighted CalEnviroScreen averages and the two system scores
public final class Scores {

    private Scores() {}

    //sum(pctl * pop) / sum(pop), a percentile of 0 is real data and counts,
    //null or negative percentiles and null or zero population are skipped
    public static Double weightedAverage(List<CensusTract> tracts, Function<CensusTract, Double> percentile) {
        double weightedSum = 0;
        long totalPopulation = 0;

        for (CensusTract tract : tracts) {
            Double value = percentile.apply(tract);
            Integer population = tract.getPopulation();
            if (value == null || value < 0 || population == null || population <= 0) {
                continue;
            }
            weightedSum += value * population;
            totalPopulation += population;
        }

        return totalPopulation == 0 ? null : weightedSum / totalPopulation;
    }

    //100 - avg DrinkingWater_Pctl, higher is cleaner
    public static Integer waterScore(Double avgDrinkingWaterPctl) {
        if (avgDrinkingWaterPctl == null) {
            return null;
        }
        long score = Math.round(100 - avgDrinkingWaterPctl);
        return (int) Math.clamp(score, 0, 100);
    }

    public static Integer roundScore(Double average) {
        return average == null ? null : (int) Math.round(average);
    }
}
