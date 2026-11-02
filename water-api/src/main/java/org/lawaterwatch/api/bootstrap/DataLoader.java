package org.lawaterwatch.api.bootstrap;

import org.lawaterwatch.api.client.EpaViolationClient;
import org.lawaterwatch.api.client.SablClient;
import org.lawaterwatch.api.dto.SablFeatureCollection;
import org.lawaterwatch.api.entity.CensusTract;
import org.lawaterwatch.api.entity.DataMeta;
import org.lawaterwatch.api.entity.Violation;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.WaterSystemRepository;
import org.lawaterwatch.api.service.GeoJsonGeometry;
import org.lawaterwatch.api.service.SystemGeometryCache;
import org.lawaterwatch.api.service.SystemScorer;
import org.lawaterwatch.api.service.TractAssigner;
import org.lawaterwatch.api.service.WaterSystemList;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Pulls SABL boundaries, CalEnviroScreen 5.0 tracts and EPA violations on startup and stores them in SQLite
//Fetch everything first, then write it all in one transaction, so a failed load keeps the old data
@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final boolean loadOnStartup;
    private final boolean forceRefresh;
    private final int maxAgeDays;

    private final SablClient sablClient;
    private final EpaViolationClient epaClient;
    private final TractAssigner tractAssigner;
    private final DataWriter dataWriter;
    private final WaterSystemRepository waterSystemRepository;
    private final ViolationRepository violationRepository;
    private final DataMetaRepository dataMetaRepository;
    private final SystemGeometryCache geometryCache;
    private final ObjectMapper objectMapper;

    public DataLoader(@Value("${app.data.load-on-startup}") boolean loadOnStartup,
                      @Value("${app.data.force-refresh}") boolean forceRefresh,
                      @Value("${app.data.max-age-days}") int maxAgeDays,
                      SablClient sablClient,
                      EpaViolationClient epaClient,
                      TractAssigner tractAssigner,
                      DataWriter dataWriter,
                      WaterSystemRepository waterSystemRepository,
                      ViolationRepository violationRepository,
                      DataMetaRepository dataMetaRepository,
                      SystemGeometryCache geometryCache,
                      ObjectMapper objectMapper) {
        this.loadOnStartup = loadOnStartup;
        this.forceRefresh = forceRefresh;
        this.maxAgeDays = maxAgeDays;
        this.sablClient = sablClient;
        this.epaClient = epaClient;
        this.tractAssigner = tractAssigner;
        this.dataWriter = dataWriter;
        this.waterSystemRepository = waterSystemRepository;
        this.violationRepository = violationRepository;
        this.dataMetaRepository = dataMetaRepository;
        this.geometryCache = geometryCache;
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
            String geometryGeoJson = objectMapper.writeValueAsString(feature.geometry());
            systems.put(entry.pwsid(), LoadMappers.toWaterSystem(entry, feature.properties(), geometryGeoJson, polygon));
            polygons.put(entry.pwsid(), polygon);
        }

        // 2. Tracts, assigned by interior point in polygon
        List<CensusTract> tracts = new ArrayList<>();
        tractAssigner.assign(polygons).forEach((tractId, assignment) ->
                tracts.add(LoadMappers.toCensusTract(tractId, assignment.pwsid(), assignment.properties())));

        // 3. Scores per system
        SystemScorer.scoreAll(systems.values(), tracts);

        // 4. Violation history, never part of the scores
        List<Violation> violations = new ArrayList<>();
        for (String pwsid : systems.keySet()) {
            violations.addAll(LoadMappers.toViolations(pwsid, epaClient.fetchViolations(pwsid)));
        }

        // 5. One transaction for every write
        dataWriter.replaceAll(systems.values(), tracts, violations);

        log.info("load finished: {} systems, {} tracts, {} violations",
                systems.size(), tracts.size(), violations.size());
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
