package org.lawaterwatch.api.client;

import org.lawaterwatch.api.dto.CesFeatureCollection;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

//CalEnviroScreen 5.0 draft layer, census tracts inside a bounding box
@Component
public class CalEnviroScreenClient {

    //The layer's maxRecordCount (checked 2026-10-21), the server never returns more per page
    private static final int PAGE_SIZE = 2000;
    //Safety stop, LA County has ~2,500 tracts so a real load never gets close
    private static final int MAX_PAGES = 50;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CalEnviroScreenClient(@Value("${app.ces.url}") String url, ObjectMapper objectMapper) {
        this.restClient = RestClients.create(url);
        this.objectMapper = objectMapper;
    }

    //Every tract whose shape intersects the box, x = lng, y = lat
    public List<CesFeatureCollection.Feature> fetchTracts(Envelope bbox) {
        String geometry = bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," + bbox.getMaxY();
        List<CesFeatureCollection.Feature> tracts = new ArrayList<>();

        for (int page = 0; page < MAX_PAGES; page++) {
            CesFeatureCollection result = fetchPage(geometry, tracts.size());
            tracts.addAll(result.features());

            //Stop on a short page, unless the server says it cut the page off early
            boolean shortPage = result.features().size() < PAGE_SIZE;
            if (result.features().isEmpty() || (shortPage && !result.moreAvailable())) {
                return tracts;
            }
        }
        throw new IllegalStateException("CalEnviroScreen paging did not finish after " + MAX_PAGES + " pages");
    }

    private CesFeatureCollection fetchPage(String geometry, int offset) {
        String body = restClient.get()
                .uri(uri -> uri
                        .queryParam("where", "1=1")
                        .queryParam("geometry", "{geometry}")
                        .queryParam("geometryType", "esriGeometryEnvelope")
                        .queryParam("inSR", "4326")
                        .queryParam("spatialRel", "esriSpatialRelIntersects")
                        .queryParam("outFields", "{outFields}")
                        .queryParam("returnGeometry", "true")
                        .queryParam("outSR", "4326")
                        //A fixed order keeps pages from overlapping or skipping rows
                        .queryParam("orderByFields", "OBJECTID")
                        .queryParam("resultOffset", offset)
                        .queryParam("resultRecordCount", PAGE_SIZE)
                        .queryParam("f", "geojson")
                        .build(geometry, CesFeatureCollection.OUT_FIELDS))
                .retrieve()
                .body(String.class);

        CesFeatureCollection result = objectMapper.readValue(body, CesFeatureCollection.class);
        //ArcGIS answers errors (e.g. "Invalid field") with 200 and {"error": ...}, which has no features
        if (result.features() == null) {
            throw new IllegalStateException("CalEnviroScreen query failed at offset " + offset + ": "
                    + ArcGis.snippet(body));
        }
        return result;
    }
}
