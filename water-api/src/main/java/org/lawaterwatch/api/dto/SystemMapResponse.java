package org.lawaterwatch.api.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.List;

//GET /api/systems/map, a GeoJSON FeatureCollection with one feature per water system
public record SystemMapResponse(String type, List<Feature> features) {

    public SystemMapResponse(List<Feature> features) {
        this("FeatureCollection", features);
    }

    //geometry is the stored GeoJSON text, @JsonRawValue writes it as a JSON object instead of a string
    public record Feature(String type, @JsonRawValue String geometry, Properties properties) {

        public Feature(String geometry, Properties properties) {
            this("Feature", geometry, properties);
        }
    }

    public record Properties(
            String pwsid,
            String name,
            Integer waterScore,
            Integer burdenScore,
            Double labelLat,
            Double labelLng,
            Integer population
    ) {}
}
