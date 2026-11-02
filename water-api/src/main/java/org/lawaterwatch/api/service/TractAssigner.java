package org.lawaterwatch.api.service;

import org.lawaterwatch.api.client.CalEnviroScreenClient;
import org.lawaterwatch.api.dto.CesFeatureCollection;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Assigns CalEnviroScreen tracts to water systems, the tract's interior point must fall inside the system polygon
@Component
public class TractAssigner {

    private static final Logger log = LoggerFactory.getLogger(TractAssigner.class);

    private final CalEnviroScreenClient cesClient;

    public TractAssigner(CalEnviroScreenClient cesClient) {
        this.cesClient = cesClient;
    }

    public record Assignment(String pwsid, CesFeatureCollection.Properties properties) {}

    //Keyed by tract id, systems are tried in the map's order so the first match wins
    public Map<String, Assignment> assign(Map<String, Geometry> polygons) {
        Map<String, Assignment> assigned = new LinkedHashMap<>();
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
                    assigned.put(tractId, new Assignment(pwsid, p));
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
}
