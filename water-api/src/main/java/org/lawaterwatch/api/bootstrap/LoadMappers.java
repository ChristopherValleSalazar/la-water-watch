package org.lawaterwatch.api.bootstrap;

import org.lawaterwatch.api.dto.CesFeatureCollection;
import org.lawaterwatch.api.dto.EpaViolation;
import org.lawaterwatch.api.dto.SablFeatureCollection;
import org.lawaterwatch.api.entity.CensusTract;
import org.lawaterwatch.api.entity.Violation;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.service.WaterSystemList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Turns the external API records into entities
final class LoadMappers {

    private static final Logger log = LoggerFactory.getLogger(LoadMappers.class);

    private LoadMappers() {}

    static WaterSystem toWaterSystem(WaterSystemList.Entry entry, SablFeatureCollection.Properties p,
                                     String geometryGeoJson, Geometry polygon) {
        WaterSystem system = new WaterSystem(entry.pwsid());
        system.setName(p.waterSystemName() != null ? p.waterSystemName() : entry.name());
        system.setCounty(p.county());
        system.setPopulation(p.population());
        system.setServiceConnections(p.serviceConnections());
        system.setPhone(p.phone());
        system.setRegulatingAgency(p.regulatingAgency());
        system.setGeometryGeoJson(geometryGeoJson);

        //Interior point, not centroid, a centroid can land outside a MultiPolygon
        Point label = polygon.getInteriorPoint();
        system.setLabelLat(label.getY());
        system.setLabelLng(label.getX());
        return system;
    }

    static CensusTract toCensusTract(String tractId, String pwsid, CesFeatureCollection.Properties p) {
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
    static Double percentile(Double value) {
        return value == null || value < 0 ? null : value;
    }

    static List<Violation> toViolations(String pwsid, List<EpaViolation> rows) {
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
    static LocalDate date(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        return LocalDate.parse(value.substring(0, 10));
    }
}
