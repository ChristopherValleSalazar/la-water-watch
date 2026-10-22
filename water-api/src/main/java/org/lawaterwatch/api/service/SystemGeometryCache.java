package org.lawaterwatch.api.service;

import org.lawaterwatch.api.entity.WaterSystem;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//The 11 service area polygons kept in memory for the address lookup, rebuilt by the DataLoader
@Service
public class SystemGeometryCache {

    private static final Logger log = LoggerFactory.getLogger(SystemGeometryCache.class);

    private final ObjectMapper objectMapper;

    //Swapped as a whole on rebuild, in WaterSystemList order so "first match wins" is predictable
    private volatile Map<String, PreparedGeometry> polygons = Map.of();

    public SystemGeometryCache(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void rebuild(List<WaterSystem> systems) {
        Map<String, PreparedGeometry> rebuilt = new LinkedHashMap<>();
        systems.stream()
                .sorted(Comparator.comparingInt(s -> WaterSystemList.PWSIDS.indexOf(s.getPwsid())))
                .forEach(system -> {
                    if (system.getGeometryGeoJson() == null) {
                        log.warn("no stored geometry for {}, it can't be found by address lookup", system.getPwsid());
                        return;
                    }
                    var geometry = GeoJsonGeometry.parse(objectMapper.readTree(system.getGeometryGeoJson()));
                    rebuilt.put(system.getPwsid(), PreparedGeometryFactory.prepare(geometry));
                });
        polygons = rebuilt;
        log.info("geometry cache rebuilt with {} systems", rebuilt.size());
    }

    //First system whose polygon covers the point, null when it's outside all of them
    //Overlaps between systems are thin border slivers (<1%), so first match wins is accurate enough
    public String findPwsid(double lat, double lng) {
        Point point = GeoJsonGeometry.point(lng, lat);
        for (Map.Entry<String, PreparedGeometry> entry : polygons.entrySet()) {
            if (entry.getValue().covers(point)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
