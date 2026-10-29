package org.lawaterwatch.api.service;

import org.lawaterwatch.api.dto.MetaResponse;
import org.lawaterwatch.api.dto.SystemDetailResponse;
import org.lawaterwatch.api.dto.SystemMapResponse;
import org.lawaterwatch.api.entity.DataMeta;
import org.lawaterwatch.api.entity.Violation;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.WaterSystemRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

//Read side for the endpoints, turns stored entities into response records
@Service
public class WaterSystemService {

    //The frontend legend names this source
    static final String SOURCE = "CalEnviroScreen 5.0 (July 2026)";

    private final WaterSystemRepository waterSystemRepository;
    private final DataMetaRepository dataMetaRepository;
    private final ViolationRepository violationRepository;

    public WaterSystemService(WaterSystemRepository waterSystemRepository, DataMetaRepository dataMetaRepository,
                              ViolationRepository violationRepository) {
        this.waterSystemRepository = waterSystemRepository;
        this.dataMetaRepository = dataMetaRepository;
        this.violationRepository = violationRepository;
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

    public SystemDetailResponse detail(String pwsid) {
        if (!WaterSystemList.contains(pwsid)) {
            throw new PwsidNotFoundException(pwsid);
        }
        WaterSystem system = waterSystemRepository.findById(pwsid)
                .orElseThrow(() -> new PwsidNotFoundException(pwsid));

        List<SystemDetailResponse.ViolationItem> violations = violationRepository.findByPwsid(pwsid).stream()
                .sorted(Comparator.comparing(Violation::getComplPerBeginDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(WaterSystemService::toViolationItem)
                .toList();

        return new SystemDetailResponse(
                system.getPwsid(),
                system.getName(),
                system.getCounty(),
                system.getPopulation(),
                system.getServiceConnections(),
                system.getPhone(),
                system.getRegulatingAgency(),
                system.getLabelLat(),
                system.getLabelLng(),
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
                violations);
    }

    private static SystemDetailResponse.ViolationItem toViolationItem(Violation violation) {
        return new SystemDetailResponse.ViolationItem(
                violation.getViolationId(),
                violation.getViolationCategoryCode(),
                violation.getIsHealthBasedInd(),
                violation.getContaminantCode(),
                "Unknown",
                violation.getComplianceStatusCode(),
                violation.getComplPerBeginDate(),
                violation.getComplPerEndDate(),
                violation.getRtcDate(),
                violation.getPublicNotificationTier());
    }

    private static SystemMapResponse.Feature toFeature(WaterSystem system) {
        return new SystemMapResponse.Feature(system.getGeometryGeoJson(), new SystemMapResponse.Properties(
                system.getPwsid(),
                system.getName(),
                system.getWaterScore(),
                system.getBurdenScore(),
                system.getLabelLat(),
                system.getLabelLng(),
                system.getPopulation()));
    }
}
