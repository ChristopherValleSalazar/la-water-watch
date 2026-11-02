package org.lawaterwatch.api.service;

import org.lawaterwatch.api.dto.MetaResponse;
import org.lawaterwatch.api.dto.SystemMapResponse;
import org.lawaterwatch.api.entity.DataMeta;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.repository.DataMetaRepository;
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

    public WaterSystemService(WaterSystemRepository waterSystemRepository, DataMetaRepository dataMetaRepository) {
        this.waterSystemRepository = waterSystemRepository;
        this.dataMetaRepository = dataMetaRepository;
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
