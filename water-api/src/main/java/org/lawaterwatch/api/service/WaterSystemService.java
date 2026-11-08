package org.lawaterwatch.api.service;

import org.lawaterwatch.api.client.CensusGeocodeClient;
import org.lawaterwatch.api.dto.*;
import org.lawaterwatch.api.entity.DataMeta;
import org.lawaterwatch.api.entity.Violation;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.exception.PwsidNotFoundException;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.ViolationRepository.ViolationCount;
import org.lawaterwatch.api.repository.WaterSystemRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Read side for the endpoints, turns stored entities into response records
@Service
public class WaterSystemService {

    //The frontend legend names this source
    static final String SOURCE = "CalEnviroScreen 5.0 (July 2026)";

    private final WaterSystemRepository waterSystemRepository;
    private final DataMetaRepository dataMetaRepository;
    private final ViolationRepository violationRepository;
    private final CensusGeocodeClient censusGeocodeClient;
    private final SystemGeometryCache systemGeometryCache;

    public WaterSystemService(WaterSystemRepository waterSystemRepository, DataMetaRepository dataMetaRepository,
                              ViolationRepository violationRepository, CensusGeocodeClient censusGeocodeClient,
                              SystemGeometryCache systemGeometryCache) {
        this.waterSystemRepository = waterSystemRepository;
        this.dataMetaRepository = dataMetaRepository;
        this.violationRepository = violationRepository;
        this.censusGeocodeClient = censusGeocodeClient;
        this.systemGeometryCache = systemGeometryCache;
    }

    public MetaResponse meta() {
        var lastRefreshed = dataMetaRepository.findById(DataMeta.SINGLETON_ID)
                .map(DataMeta::getLastRefreshed)
                .orElse(null);
        return new MetaResponse(lastRefreshed, SOURCE);
    }

    public SystemMapResponse map() {
        List<SystemMapResponse.Feature> features = waterSystemRepository.findAll().stream()
                .filter(system -> system.getGeometryGeoJson() != null)
                .sorted(Comparator.comparingInt(system -> WaterSystemList.PWSIDS.indexOf(system.getPwsid())))
                .map(WaterSystemService::toFeature)
                .toList();
        return new SystemMapResponse(features);
    }

    private static SystemMapResponse.Feature toFeature(WaterSystem system) {
        return new SystemMapResponse.Feature(GeoJsonGeometry.roundCoordinates(system.getGeometryGeoJson()), new SystemMapResponse.Properties(
                system.getPwsid(),
                system.getName(),
                system.getWaterScore(),
                system.getBurdenScore(),
                system.getLabelLat(),
                system.getLabelLng(),
                system.getPopulation()));
    }

    public SystemInfoPanel infoPanel(String pwsid) {
        if(!WaterSystemList.contains(pwsid)) {
            throw new PwsidNotFoundException(pwsid);
        }

        WaterSystem system = waterSystemRepository.findById(pwsid)
                .orElseThrow(() -> new PwsidNotFoundException(pwsid));

        List<SystemInfoPanel.ViolationItem> violations =
                violationRepository.findByPwsid(pwsid).stream()
                        .sorted(Comparator.comparing(Violation::getComplPerBeginDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(WaterSystemService::toViolationItem)
                        .toList();

        return new SystemInfoPanel(
            system.getPwsid(),
            system.getName(),
            system.getCounty(),
            system.getPopulation(),
            system.getServiceConnections(),
            system.getPhone(),
            system.getRegulatingAgency(),
            system.getLabelLat(), //Y-axis
            system.getLabelLng(), //X-axis
            system.getWaterScore(),
            system.getBurdenScore(),
            system.getTractCount(),
            system.getTractPopulation(),
            system.getAvgAsthmaPctl(),
            system.getAvgPovertyPctl(),
            system.getAvgChildLeadPctl(),
            system.getAvgToxReleasesPctl(),
            system.getAvgGwThreatsPctl(),
            system.getAvgCleanupSitesPctl(),
            system.getAvgAirPm25Pctl(),
            violations
        );
    }

    private static SystemInfoPanel.ViolationItem toViolationItem(Violation violation) {
        return new SystemInfoPanel.ViolationItem(
                violation.getViolationId(),
                violation.getViolationCategoryCode(),
                violation.getIsHealthBasedInd(),
                violation.getContaminantCode(),
                "Unkown",
                violation.getComplianceStatusCode(),
                violation.getComplPerBeginDate(),
                violation.getComplPerEndDate(),
                violation.getRtcDate(),
                violation.getPublicNotificationTier()
        );
    }



    public List<LeaderboardRow> leaderboard(Sort sort) {
        Map<String, Long> violationCounts = violationRepository.countGroupedByPwsid().stream()
                .collect(Collectors.toMap(ViolationCount::getPwsid, ViolationCount::getViolationCount));

        Stream<LeaderboardRow> rows = waterSystemRepository.findAll().stream()
                .map(system -> toLeaderboardRow(system, violationCounts.getOrDefault(system.getPwsid(), 0L)));

        Comparator<LeaderboardRow> order = switch(sort) {
            case NONE -> null;
            //Best first: high water score, low burden, few violations; nulls always at the bottom
            case WATERSCORE  -> Comparator.comparing(LeaderboardRow::waterScore,  Comparator.nullsLast(Comparator.reverseOrder()));
            case VIOLATIONS  -> Comparator.comparing(LeaderboardRow::violations,  Comparator.naturalOrder());
            case BURDENSCORE -> Comparator.comparing(LeaderboardRow::burdenScore, Comparator.nullsLast(Comparator.naturalOrder()));
            case POPULATION  -> Comparator.comparing(LeaderboardRow::population,  Comparator.nullsLast(Comparator.reverseOrder()));
            case TRACT       -> Comparator.comparing(LeaderboardRow::tracts,      Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if(order != null) {
            rows = rows.sorted(order.thenComparing(LeaderboardRow::name,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return rows.toList();
    }

    private static LeaderboardRow toLeaderboardRow(WaterSystem system, long violations) {
        return new LeaderboardRow(
                system.getPwsid(),
                system.getName(),
                system.getWaterScore(),
                system.getBurdenScore(),
                system.getPopulation(),
                system.getTractCount(),
                violations
        );
    }

    public AvgScoreResponse avgScore() {
        List<WaterSystem> systems = waterSystemRepository.findAll();
        //Fixed list size, so the average is over every system we track
        double systemCount = WaterSystemList.PWSIDS.size();

        //Null scores are skipped in the sum, unboxing them would throw
        int waterTotal = systems.stream()
                .map(WaterSystem::getWaterScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int burdenTotal = systems.stream()
                .map(WaterSystem::getBurdenScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        //Round instead of integer division so 67.6 shows as 68, not 67
        return new AvgScoreResponse(
                (int) Math.round(waterTotal / systemCount),
                (int) Math.round(burdenTotal / systemCount));
    }

    //Geocode the address, then find which system polygon covers it
    //Not found by the geocoder gives all nulls, found but outside every system gives coordinates with a null pwsid
    public AddressLookupResponse addressLookup(String address) {
        GeoCodeResponse.AddressMatch match = censusGeocodeClient.geocode(address);
        if (match == null || match.coordinates() == null) {
            return AddressLookupResponse.notFound();
        }

        double lat = match.coordinates().y();
        double lng = match.coordinates().x();
        return new AddressLookupResponse(match.matchedAddress(), lat, lng, systemGeometryCache.findPwsid(lat, lng));
    }
}
