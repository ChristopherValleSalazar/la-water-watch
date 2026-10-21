package org.lawaterwatch.api.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import tools.jackson.databind.JsonNode;

//Turns a GeoJSON Polygon or MultiPolygon into a JTS geometry, x = lng, y = lat
public final class GeoJsonGeometry {

    //SRID 4326 = WGS84 lng/lat, what SABL and CES return with outSR=4326
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoJsonGeometry() {}

    public static Geometry parse(JsonNode geometry) {
        if (geometry == null || geometry.isNull()) {
            throw new IllegalArgumentException("geometry is missing");
        }

        String type = geometry.path("type").asString();
        JsonNode coordinates = geometry.path("coordinates");

        Geometry parsed = switch (type) {
            case "Polygon" -> polygon(coordinates);
            case "MultiPolygon" -> multiPolygon(coordinates);
            default -> throw new IllegalArgumentException("unsupported geometry type: " + type);
        };

        //Public boundary data often has self intersections, repair instead of failing
        return parsed.isValid() ? parsed : GeometryFixer.fix(parsed);
    }

    private static Geometry multiPolygon(JsonNode polygons) {
        Polygon[] parts = new Polygon[polygons.size()];
        for (int i = 0; i < polygons.size(); i++) {
            parts[i] = polygon(polygons.get(i));
        }
        return FACTORY.createMultiPolygon(parts);
    }

    //First ring is the outer shell, any others are holes
    private static Polygon polygon(JsonNode rings) {
        LinearRing shell = ring(rings.get(0));
        LinearRing[] holes = new LinearRing[rings.size() - 1];
        for (int i = 1; i < rings.size(); i++) {
            holes[i - 1] = ring(rings.get(i));
        }
        return FACTORY.createPolygon(shell, holes);
    }

    private static LinearRing ring(JsonNode points) {
        int size = points.size();
        Coordinate first = coordinate(points.get(0));
        Coordinate last = coordinate(points.get(size - 1));
        boolean open = !first.equals2D(last);

        Coordinate[] coords = new Coordinate[open ? size + 1 : size];
        for (int i = 0; i < size; i++) {
            coords[i] = coordinate(points.get(i));
        }
        //JTS requires closed rings, GeoJSON sources don't always close them
        if (open) {
            coords[size] = first.copy();
        }
        return FACTORY.createLinearRing(coords);
    }

    //GeoJSON positions are [lng, lat]
    private static Coordinate coordinate(JsonNode position) {
        return new Coordinate(position.get(0).asDouble(), position.get(1).asDouble());
    }
}
