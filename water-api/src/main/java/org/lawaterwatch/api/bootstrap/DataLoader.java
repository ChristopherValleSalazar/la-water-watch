package org.lawaterwatch.api.bootstrap;

import org.lawaterwatch.api.client.CalEnviroScreenClient;
import org.lawaterwatch.api.client.EpaViolationClient;
import org.lawaterwatch.api.client.SablClient;
import org.lawaterwatch.api.dto.CesFeatureCollection;
import org.lawaterwatch.api.dto.EpaViolation;
import org.lawaterwatch.api.dto.SablFeatureCollection;
import org.lawaterwatch.api.entity.CensusTract;
import org.lawaterwatch.api.entity.DataMeta;
import org.lawaterwatch.api.entity.Violation;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.repository.CensusTractRepository;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.WaterSystemRepository;
import org.lawaterwatch.api.service.GeoJsonGeometry;
import org.lawaterwatch.api.service.Scores;
import org.lawaterwatch.api.service.SystemGeometryCache;
import org.lawaterwatch.api.service.WaterSystemList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

//Pulls SABL boundaries, CalEnviroScreen 5.0 tracts and EPA violations on startup and stores them in SQLite
//Fetch everything first, then write it all in one transaction, so a failed load keeps the old data
@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final boolean loadOnStartup;
    private final boolean forceRefresh;
    private final int maxAgeDays;

    private final SablClient sablClient;
    private final CalEnviroScreenClient cesClient;
    private final EpaViolationClient epaClient;
    private final WaterSystemRepository waterSystemRepository;
    private final CensusTractRepository censusTractRepository;
    private final ViolationRepository violationRepository;
    private final DataMetaRepository dataMetaRepository;
    private final SystemGeometryCache geometryCache;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public DataLoader(@Value("${app.data.load-on-startup}") boolean loadOnStartup,
                      @Value("${app.data.force-refresh}") boolean forceRefresh,
                      @Value("${app.data.max-age-days}") int maxAgeDays,
                      SablClient sablClient,
                      CalEnviroScreenClient cesClient,
                      EpaViolationClient epaClient,
                      WaterSystemRepository waterSystemRepository,
                      CensusTractRepository censusTractRepository,
                      ViolationRepository violationRepository,
                      DataMetaRepository dataMetaRepository,
                      SystemGeometryCache geometryCache,
                      TransactionTemplate transactionTemplate,
                      ObjectMapper objectMapper) {
        this.loadOnStartup = loadOnStartup;
        this.forceRefresh = forceRefresh;
        this.maxAgeDays = maxAgeDays;
        this.sablClient = sablClient;
        this.cesClient = cesClient;
        this.epaClient = epaClient;
        this.waterSystemRepository = waterSystemRepository;
        this.censusTractRepository = censusTractRepository;
        this.violationRepository = violationRepository;
        this.dataMetaRepository = dataMetaRepository;
        this.geometryCache = geometryCache;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!loadOnStartup) {
            log.info("load-on-startup is off, using the data already in the database");
        } else if (!forceRefresh && isFresh()) {
            log.info("data is fresh, skipping load");
        } else {
            try {
                load();
            } catch (RuntimeException e) {
                log.error("data load failed, keeping the existing data", e);
            }
        }

        //Always from the database, so it matches what the endpoints serve whichever branch ran
        List<WaterSystem> systems = waterSystemRepository.findAll();
        geometryCache.rebuild(systems);
        logSummary(systems);
    }

    private boolean isFresh() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(maxAgeDays));
        return dataMetaRepository.findById(DataMeta.SINGLETON_ID)
                .map(DataMeta::getLastRefreshed)
                .filter(lastRefreshed -> lastRefreshed.isAfter(cutoff))
                .isPresent();
    }

    private void load() {
        log.info("loading data from SABL, CalEnviroScreen 5.0 and EPA");

        // 1. Boundaries, in WaterSystemList order so the first match wins for tracts
        Map<String, SablFeatureCollection.Feature> boundaries = sablClient.fetchBoundaries(WaterSystemList.PWSIDS);
        Map<String, WaterSystem> systems = new LinkedHashMap<>();
        Map<String, Geometry> polygons = new LinkedHashMap<>();

        for (WaterSystemList.Entry entry : WaterSystemList.ALL) {
            SablFeatureCollection.Feature feature = boundaries.get(entry.pwsid());
            if (feature == null) {
                log.warn("SABL has no boundary for {} ({}), skipping it", entry.name(), entry.pwsid());
                continue;
            }
            Geometry polygon = GeoJsonGeometry.parse(feature.geometry());
            systems.put(entry.pwsid(), toWaterSystem(entry, feature, polygon));
            polygons.put(entry.pwsid(), polygon);
        }

        // 2. Tracts, assigned by interior point in polygon
        Map<String, CensusTract> tracts = assignTracts(polygons);

        // 3. Scores per system
        Map<String, List<CensusTract>> tractsBySystem = new LinkedHashMap<>();
        tracts.values().forEach(t -> tractsBySystem.computeIfAbsent(t.getPwsid(), k -> new ArrayList<>()).add(t));
        systems.values().forEach(system ->
                applyScores(system, tractsBySystem.getOrDefault(system.getPwsid(), List.of())));

        // 4. Violation history, never part of the scores
        List<Violation> violations = new ArrayList<>();
        for (String pwsid : systems.keySet()) {
            violations.addAll(toViolations(pwsid, epaClient.fetchViolations(pwsid)));
        }

        // 5. One transaction for every write
        transactionTemplate.executeWithoutResult(status -> {
            violationRepository.deleteAllInBatch();
            censusTractRepository.deleteAllInBatch();
            waterSystemRepository.deleteAllInBatch();

            waterSystemRepository.saveAll(systems.values());
            censusTractRepository.saveAll(tracts.values());
            violationRepository.saveAll(violations);

            DataMeta meta = dataMetaRepository.findById(DataMeta.SINGLETON_ID).orElseGet(DataMeta::new);
            meta.setLastRefreshed(Instant.now());
            dataMetaRepository.save(meta);
        });

        log.info("load finished: {} systems, {} tracts, {} violations",
                systems.size(), tracts.size(), violations.size());
    }

    private WaterSystem toWaterSystem(WaterSystemList.Entry entry, SablFeatureCollection.Feature feature,
                                      Geometry polygon) {
        SablFeatureCollection.Properties p = feature.properties();
        WaterSystem system = new WaterSystem(entry.pwsid());
        system.setName(p.waterSystemName() != null ? p.waterSystemName() : entry.name());
        system.setCounty(p.county());
        system.setPopulation(p.population());
        system.setServiceConnections(p.serviceConnections());
        system.setPhone(p.phone());
        system.setRegulatingAgency(p.regulatingAgency());
        system.setGeometryGeoJson(objectMapper.writeValueAsString(feature.geometry()));

        //Interior point, not centroid, a centroid can land outside a MultiPolygon
        Point label = polygon.getInteriorPoint();
        system.setLabelLat(label.getY());
        system.setLabelLng(label.getX());
        return system;
    }

    private Map<String, CensusTract> assignTracts(Map<String, Geometry> polygons) {
        Map<String, CensusTract> assigned = new LinkedHashMap<>();
        int unparseable = 0;

        for (Map.Entry<String, Geometry> system : polygons.entrySet()) {
            String pwsid = system.getKey();
            PreparedGeometry polygon = PreparedGeometryFactory.prepare(system.getValue());
            List<CesFeatureCollection.Feature> candidates = cesClient.fetchTracts(system.getValue().getEnvelopeInternal());
            int matched = 0;

            for (CesFeatureCollection.Feature feature : candidates) {
                CesFeatureCollection.Properties p = feature.properties();
                String tractId = p == null ? null : p.tractId();
                //Already assigned to an earlier system, first match wins
                if (tractId == null || assigned.containsKey(tractId)
                        || feature.geometry() == null || feature.geometry().isNull()) {
                    continue;
                }

                Point interior;
                try {
                    interior = GeoJsonGeometry.parse(feature.geometry()).getInteriorPoint();
                } catch (RuntimeException e) {
                    unparseable++;
                    continue;
                }
                if (polygon.covers(interior)) {
                    assigned.put(tractId, toCensusTract(tractId, pwsid, p));
                    matched++;
                }
            }
            log.info("{}: {} tracts in bounding box, {} assigned", pwsid, candidates.size(), matched);
        }

        if (unparseable > 0) {
            log.warn("{} tract shapes could not be parsed and were skipped", unparseable);
        }
        return assigned;
    }

    private static CensusTract toCensusTract(String tractId, String pwsid, CesFeatureCollection.Properties p) {
        CensusTract tract = new CensusTract(tractId, pwsid);
        tract.setCity(p.appoxLoc());
        tract.setPopulation(p.population());
        tract.setDrinkingWater(p.drinkingWater());
        tract.setDrinkingWaterPctl(percentile(p.drinkingWaterPctl()));
        tract.setCiScorePctl(percentile(p.ciScorePctl()));
        tract.setPollutionPctl(percentile(p.pollutionPctl()));
        tract.setPopCharPctl(percentile(p.popCharPctl()));
        tract.setPovertyPctl(percentile(p.povertyPctl()));
        tract.setAsthmaPctl(percentile(p.asthmaPctl()));
        tract.setChildLeadPctl(percentile(p.childLeadPctl()));
        tract.setToxReleasesPctl(percentile(p.toxReleasesPctl()));
        tract.setGwThreatsPctl(percentile(p.gwThreatsPctl()));
        tract.setCleanupSitesPctl(percentile(p.cleanupSitesPctl()));
        tract.setAirPm25Pctl(percentile(p.airPm25Pctl()));
        return tract;
    }

    //Negative values are CES "no data" sentinels, 0 is a real percentile and is kept
    private static Double percentile(Double value) {
        return value == null || value < 0 ? null : value;
    }

    private static void applyScores(WaterSystem system, List<CensusTract> tracts) {
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

        system.setWaterScore(Scores.waterScore(avg(tracts, CensusTract::getDrinkingWaterPctl)));
        system.setBurdenScore(Scores.roundScore(avg(tracts, CensusTract::getCiScorePctl)));
        system.setAvgAsthmaPctl(avg(tracts, CensusTract::getAsthmaPctl));
        system.setAvgPovertyPctl(avg(tracts, CensusTract::getPovertyPctl));
        system.setAvgChildLeadPctl(avg(tracts, CensusTract::getChildLeadPctl));
        system.setAvgToxReleasesPctl(avg(tracts, CensusTract::getToxReleasesPctl));
        system.setAvgGwThreatsPctl(avg(tracts, CensusTract::getGwThreatsPctl));
        system.setAvgCleanupSitesPctl(avg(tracts, CensusTract::getCleanupSitesPctl));
        system.setAvgAirPm25Pctl(avg(tracts, CensusTract::getAirPm25Pctl));
    }

    private static Double avg(List<CensusTract> tracts, Function<CensusTract, Double> percentile) {
        return Scores.weightedAverage(tracts, percentile);
    }

    private static List<Violation> toViolations(String pwsid, List<EpaViolation> rows) {
        List<Violation> violations = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        int duplicates = 0;

        for (EpaViolation row : rows) {
            if (row.violationId() != null && !seenIds.add(row.violationId())) {
                duplicates++;
            }
            Violation violation = new Violation(row.violationId(), pwsid);
            violation.setViolationCategoryCode(row.violationCategoryCode());
            violation.setIsHealthBasedInd(row.isHealthBasedInd());
            violation.setContaminantCode(row.contaminantCode());
            violation.setComplianceStatusCode(row.complianceStatusCode());
            violation.setComplPerBeginDate(date(row.complPerBeginDate()));
            violation.setComplPerEndDate(date(row.complPerEndDate()));
            violation.setRtcDate(date(row.rtcDate()));
            violation.setPublicNotificationTier(row.publicNotificationTier());
            violations.add(violation);
        }

        //Stored anyway, the surrogate key means nothing is overwritten
        if (duplicates > 0) {
            log.warn("{}: EPA returned {} rows with a repeated violation_id", pwsid, duplicates);
        }
        return violations;
    }

    //"2016-01-20 00:00:00" -> 2016-01-20
    private static LocalDate date(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    private void logSummary(List<WaterSystem> systems) {
        if (systems.isEmpty()) {
            log.warn("no water systems in the database");
            return;
        }
        StringBuilder table = new StringBuilder(String.format("%n%-10s  %-45s  %6s  %5s  %6s  %10s%n",
                "pwsid", "name", "tracts", "water", "burden", "violations"));
        systems.stream()
                .sorted(Comparator.comparingInt(s -> WaterSystemList.PWSIDS.indexOf(s.getPwsid())))
                .forEach(s -> table.append(String.format("%-10s  %-45.45s  %6s  %5s  %6s  %10d%n",
                        s.getPwsid(), s.getName(), s.getTractCount(), s.getWaterScore(), s.getBurdenScore(),
                        violationRepository.countByPwsid(s.getPwsid()))));
        log.info("water systems:{}", table);
    }
}
