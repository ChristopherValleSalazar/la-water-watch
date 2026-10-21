package org.lawaterwatch.api.service;

import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.entity.CensusTract;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//Step 2 check: plain helpers, no Spring context
class HelpersTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String text) {
        return mapper.readTree(text);
    }

    private static CensusTract tract(String id, Integer population, Double drinkingWaterPctl) {
        CensusTract tract = new CensusTract(id, "CA1910067");
        tract.setPopulation(population);
        tract.setDrinkingWaterPctl(drinkingWaterPctl);
        return tract;
    }

    // GeoJsonGeometry

    @Test
    void squareInteriorPointIsInside() {
        //Ring left open on purpose, the parser must close it
        Geometry square = GeoJsonGeometry.parse(json("""
                {"type": "Polygon", "coordinates": [[
                  [-118.3, 34.0], [-118.2, 34.0], [-118.2, 34.1], [-118.3, 34.1]
                ]]}
                """));

        Point interior = square.getInteriorPoint();

        assertTrue(square.isValid());
        assertEquals(4326, square.getSRID());
        assertTrue(square.covers(interior));
        //x is longitude, y is latitude
        assertTrue(interior.getX() < -118.2 && interior.getX() > -118.3);
        assertTrue(interior.getY() > 34.0 && interior.getY() < 34.1);
    }

    @Test
    void multiPolygonKeepsEveryPart() {
        Geometry geometry = GeoJsonGeometry.parse(json("""
                {"type": "MultiPolygon", "coordinates": [
                  [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]],
                  [[[5, 5], [6, 5], [6, 6], [5, 6], [5, 5]]]
                ]}
                """));

        assertEquals(2, geometry.getNumGeometries());
    }

    @Test
    void invalidShapeIsRepaired() {
        //Bowtie, the ring crosses itself
        Geometry bowtie = GeoJsonGeometry.parse(json("""
                {"type": "Polygon", "coordinates": [[[0, 0], [2, 2], [2, 0], [0, 2], [0, 0]]]}
                """));

        assertTrue(bowtie.isValid());
    }

    @Test
    void unsupportedTypeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GeoJsonGeometry.parse(json("""
                        {"type": "Point", "coordinates": [0, 0]}
                        """)));
    }

    // Scores

    @Test
    void zeroPercentileCountsInWeightedAverage() {
        List<CensusTract> tracts = List.of(
                tract("06037000001", 1000, 0.0),
                tract("06037000002", 3000, 80.0));

        //(0 * 1000 + 80 * 3000) / 4000 = 60, skipping the 0 would give 80, unweighted would give 40
        assertEquals(60.0, Scores.weightedAverage(tracts, CensusTract::getDrinkingWaterPctl), 1e-9);
    }

    @Test
    void missingValuesAreSkipped() {
        List<CensusTract> tracts = List.of(
                tract("06037000001", 2000, 50.0),
                tract("06037000002", 5000, null),   // no percentile
                tract("06037000003", 5000, -999.0), // sentinel
                tract("06037000004", null, 90.0),   // no population
                tract("06037000005", 0, 90.0));     // zero population

        assertEquals(50.0, Scores.weightedAverage(tracts, CensusTract::getDrinkingWaterPctl), 1e-9);
    }

    @Test
    void noUsableTractsGivesNull() {
        assertNull(Scores.weightedAverage(List.of(), CensusTract::getDrinkingWaterPctl));
        assertNull(Scores.waterScore(null));
        assertNull(Scores.roundScore(null));
    }

    @Test
    void waterScoreIsHundredMinusAverageClamped() {
        assertEquals(62, Scores.waterScore(38.0345168736946));
        assertEquals(100, Scores.waterScore(0.0));
        assertEquals(0, Scores.waterScore(100.0));
        assertEquals(100, Scores.waterScore(-5.0));
        assertEquals(0, Scores.waterScore(105.0));
    }

    @Test
    void roundScoreRoundsHalfUp() {
        assertEquals(39, Scores.roundScore(38.54247424964));
        assertEquals(31, Scores.roundScore(31.4763539705394));
    }

    // WaterSystemList

    @Test
    void elevenSystemsWithUniquePwsids() {
        assertEquals(11, WaterSystemList.ALL.size());
        assertEquals(11, WaterSystemList.PWSIDS.stream().distinct().count());
        assertTrue(WaterSystemList.contains("CA1910067"));
        assertFalse(WaterSystemList.contains("CA0000000"));
    }
}
