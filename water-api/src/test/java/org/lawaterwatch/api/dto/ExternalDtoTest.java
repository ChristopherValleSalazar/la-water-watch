package org.lawaterwatch.api.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//Step 3 check: the records map real response shapes (copied from the live APIs, trimmed down)
class ExternalDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sablTrimsPaddedStrings() {
        SablFeatureCollection result = mapper.readValue("""
                {"type": "FeatureCollection", "features": [{
                  "type": "Feature", "id": 8879623,
                  "geometry": {"type": "Polygon", "coordinates": [[[-118.4, 34.07], [-118.39, 34.07], [-118.39, 34.08], [-118.4, 34.07]]]},
                  "properties": {
                    "SABL_PWSID": "CA1910156", "BOUNDARY_TYPE": "Water Service Area",
                    "WATER_SYSTEM_NAME": "BEVERLY HILLS-CITY, WATER DEPT.", "COUNTY": "LOS ANGELES",
                    "POPULATION": 39922, "SERVICE_CONNECTIONS": 10522,
                    "REGULATING_AGENCY": "DISTRICT 22 - ANGELES",
                    "AC_PHONE_NUMBER": "310-285-2483                  ", "AC_EMAIL": "x@example.org"
                  }
                }]}
                """, SablFeatureCollection.class);

        SablFeatureCollection.Properties p = result.features().getFirst().properties();
        assertEquals("CA1910156", p.pwsid());
        assertEquals("310-285-2483", p.phone());
        assertEquals(10522, p.serviceConnections());
        assertEquals("Polygon", result.features().getFirst().geometry().path("type").asString());
    }

    @Test
    void cesPadsTractAndKeepsZeroPercentile() {
        CesFeatureCollection result = mapper.readValue("""
                {"type": "FeatureCollection", "properties": {"exceededTransferLimit": true}, "features": [{
                  "type": "Feature", "geometry": null,
                  "properties": {
                    "tract": 6037101122, "AppoxLoc": "Tujunga", "Population": 4198,
                    "DrinkingWater_Pctl": 38.0345168736946, "CIscore_Pctl": 31.4763539705394,
                    "GWThreats_Pctl": 0, "AirPM25_Pctl": 46.881177245772
                  }
                }]}
                """, CesFeatureCollection.class);

        CesFeatureCollection.Properties p = result.features().getFirst().properties();
        assertEquals("06037101122", p.tractId());
        assertEquals(4198, p.population());
        assertEquals(0.0, p.gwThreatsPctl());
        assertNull(p.asthmaPctl());
        assertTrue(result.moreAvailable());
    }

    @Test
    void cesLastPageHasNoMore() {
        CesFeatureCollection result = mapper.readValue("""
                {"type": "FeatureCollection", "features": []}
                """, CesFeatureCollection.class);

        assertFalse(result.moreAvailable());
    }

    @Test
    void epaRowsMapAndEmptyArrayIsEmpty() {
        List<EpaViolation> rows = List.of(mapper.readValue("""
                [{"pwsid": "CA1910156", "violation_id": "722071", "violation_code": "27",
                  "violation_category_code": "MR", "is_health_based_ind": "N", "contaminant_code": "2950",
                  "compliance_status_code": "R", "compl_per_begin_date": "2023-01-01 00:00:00",
                  "compl_per_end_date": "2023-03-31 00:00:00", "rtc_date": "2024-07-23 00:00:00",
                  "public_notification_tier": 3, "rule_code": "220"}]
                """, EpaViolation[].class));

        assertEquals(1, rows.size());
        assertEquals("722071", rows.getFirst().violationId());
        assertEquals(3, rows.getFirst().publicNotificationTier());
        assertEquals(0, mapper.readValue("[]", EpaViolation[].class).length);
    }
}
