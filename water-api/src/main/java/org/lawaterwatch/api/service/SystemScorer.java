package org.lawaterwatch.api.service;

import org.lawaterwatch.api.entity.CensusTract;
import org.lawaterwatch.api.entity.WaterSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Fills each system's tract totals, scores and avg percentiles from its assigned tracts
public final class SystemScorer {

    private static final Logger log = LoggerFactory.getLogger(SystemScorer.class);

    private SystemScorer() {}

    public static void scoreAll(Collection<WaterSystem> systems, Collection<CensusTract> tracts) {
        Map<String, List<CensusTract>> tractsBySystem = new LinkedHashMap<>();
        tracts.forEach(t -> tractsBySystem.computeIfAbsent(t.getPwsid(), k -> new ArrayList<>()).add(t));
        systems.forEach(system -> score(system, tractsBySystem.getOrDefault(system.getPwsid(), List.of())));
    }

    public static void score(WaterSystem system, List<CensusTract> tracts) {
        system.setTractCount(tracts.size());
        system.setTractPopulation(tracts.stream()
                .map(CensusTract::getPopulation)
                .filter(pop -> pop != null && pop > 0)
                .mapToLong(Integer::longValue)
                .sum());

        if (tracts.isEmpty()) {
            log.warn("{} ({}) has no matched tracts, its scores stay empty", system.getName(), system.getPwsid());
            return;
        }

        system.setWaterScore(Scores.waterScore(Scores.weightedAverage(tracts, CensusTract::getDrinkingWaterPctl)));
        system.setBurdenScore(Scores.roundScore(Scores.weightedAverage(tracts, CensusTract::getCiScorePctl)));
        system.setAvgAsthmaPctl(Scores.weightedAverage(tracts, CensusTract::getAsthmaPctl));
        system.setAvgPovertyPctl(Scores.weightedAverage(tracts, CensusTract::getPovertyPctl));
        system.setAvgChildLeadPctl(Scores.weightedAverage(tracts, CensusTract::getChildLeadPctl));
        system.setAvgToxReleasesPctl(Scores.weightedAverage(tracts, CensusTract::getToxReleasesPctl));
        system.setAvgGwThreatsPctl(Scores.weightedAverage(tracts, CensusTract::getGwThreatsPctl));
        system.setAvgCleanupSitesPctl(Scores.weightedAverage(tracts, CensusTract::getCleanupSitesPctl));
        system.setAvgAirPm25Pctl(Scores.weightedAverage(tracts, CensusTract::getAirPm25Pctl));
    }
}
