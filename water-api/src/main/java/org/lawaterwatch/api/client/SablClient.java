package org.lawaterwatch.api.client;

import org.lawaterwatch.api.dto.SablFeatureCollection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//State Water Board service area boundaries (SABL)
@Component
public class SablClient {

    private static final String PREFERRED_BOUNDARY_TYPE = "Water Service Area";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SablClient(@Value("${app.sabl.url}") String url, ObjectMapper objectMapper) {
        this.restClient = RestClients.create(url);
        this.objectMapper = objectMapper;
    }

    //One query for every pwsid, returns one boundary per pwsid that SABL has
    public Map<String, SablFeatureCollection.Feature> fetchBoundaries(List<String> pwsids) {
        String where = pwsids.stream()
                .map(pwsid -> "'" + pwsid + "'")
                .collect(Collectors.joining(",", "SABL_PWSID IN (", ")"));

        //Body read as String, ArcGIS doesn't always send a JSON content type
        String body = restClient.get()
                .uri(uri -> uri
                        .queryParam("where", "{where}")
                        .queryParam("outFields", "*")
                        .queryParam("outSR", "4326")
                        .queryParam("f", "geojson")
                        .build(where))
                .retrieve()
                .body(String.class);

        SablFeatureCollection result = objectMapper.readValue(body, SablFeatureCollection.class);
        //ArcGIS answers errors with 200 and {"error": ...}, which has no features
        if (result.features() == null) {
            throw new IllegalStateException("SABL query failed: " + ArcGis.snippet(body));
        }
        return pickBoundaries(result.features());
    }

    //A pwsid can have several features, prefer the Water Service Area one, otherwise keep the first
    static Map<String, SablFeatureCollection.Feature> pickBoundaries(List<SablFeatureCollection.Feature> features) {
        Map<String, SablFeatureCollection.Feature> picked = new LinkedHashMap<>();
        for (SablFeatureCollection.Feature feature : features) {
            if (feature.properties() == null || feature.properties().pwsid() == null || feature.geometry() == null
                    || feature.geometry().isNull()) {
                continue;
            }
            String pwsid = feature.properties().pwsid();
            SablFeatureCollection.Feature current = picked.get(pwsid);
            if (current == null || (!isPreferred(current) && isPreferred(feature))) {
                picked.put(pwsid, feature);
            }
        }
        return picked;
    }

    private static boolean isPreferred(SablFeatureCollection.Feature feature) {
        return PREFERRED_BOUNDARY_TYPE.equalsIgnoreCase(feature.properties().boundaryType());
    }
}
