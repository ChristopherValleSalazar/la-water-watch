package org.lawaterwatch.api.dto;

import java.time.LocalDate;
import java.util.List;

public record SystemDetailResponse(
        String pwsid,
        String name,
        String county,
        Integer population,
        Integer serviceConnections,
        String phone,
        String regulatingAgency,
        Double labelLat,
        Double labelLng,
        Integer waterScore,
        Integer burdenScore,
        Integer tractCount,
        Long tractPopulation,
        Double avgAsthmaPctl,
        Double avgPovertyPctl,
        Double avgChildLeadPctl,
        Double avgToxReleasesPctl,
        Double avgGwThreatsPctl,
        Double avgCleanupSitesPctl,
        Double avgAirPm25Pctl,
        List<ViolationItem> violations
) {

    public record ViolationItem(
            String violationId,
            String violationCategoryCode,
            String isHealthBasedInd,
            String contaminantCode,
            String contaminantName,
            String complianceStatusCode,
            LocalDate complPerBeginDate,
            LocalDate complPerEndDate,
            LocalDate rtcDate,
            Integer publicNotificationTier
    ) {}
}
