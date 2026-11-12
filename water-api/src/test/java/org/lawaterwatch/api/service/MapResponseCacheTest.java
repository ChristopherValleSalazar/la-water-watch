package org.lawaterwatch.api.service;

import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.dto.SystemMapResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//Map bytes built once, gzip round trips, coordinates rounded for display only
class MapResponseCacheTest {

    private final WaterSystemService waterSystemService = mock(WaterSystemService.class);
    private final MapResponseCache cache = new MapResponseCache(waterSystemService, JsonMapper.builder().build());

    private static SystemMapResponse oneSystem(String name) {
        return new SystemMapResponse(List.of(new SystemMapResponse.Feature(
                "{\"type\":\"Polygon\",\"coordinates\":[[[-118.5,34.0],[-118.4,34.0],[-118.4,34.1],[-118.5,34.0]]]}",
                new SystemMapResponse.Properties("CA1910067", name, 80, 40, 34.05, -118.45, 1000))));
    }

    @Test
    void gzipUnzipsToTheSameJson() throws Exception {
        when(waterSystemService.map()).thenReturn(oneSystem("LADWP"));

        MapResponseCache.Body body = cache.get();

        try (var in = new GZIPInputStream(new ByteArrayInputStream(body.gzip()))) {
            assertThat(in.readAllBytes()).isEqualTo(body.json());
        }
        assertThat(new String(body.json())).contains("\"name\":\"LADWP\"", "\"coordinates\":[[[-118.5,34.0]");
    }

    @Test
    void builtOnceAndSharedUntilRebuild() {
        when(waterSystemService.map()).thenReturn(oneSystem("LADWP"));

        MapResponseCache.Body first = cache.get();
        assertThat(cache.get()).isSameAs(first);
        verify(waterSystemService, times(1)).map();

        when(waterSystemService.map()).thenReturn(oneSystem("Renamed"));
        cache.rebuild();
        assertThat(cache.get().etag()).isNotEqualTo(first.etag());
        assertThat(cache.get().gzipEtag()).isEqualTo(cache.get().etag() + "-gz");
    }

    @Test
    void coordinatesRoundedToFiveDecimals() {
        String rounded = GeoJsonGeometry.roundCoordinates(
                "{\"type\":\"Polygon\",\"coordinates\":[[[-118.50784803892704,34.03739053116484],[-118.4,34.000004999]]]}");

        assertThat(rounded).isEqualTo("{\"type\":\"Polygon\",\"coordinates\":[[[-118.50785,34.03739],[-118.4,34]]]}");
    }

    @Test
    void shortNumbersAndNullUntouched() {
        assertThat(GeoJsonGeometry.roundCoordinates("[1.5,-2.12345]")).isEqualTo("[1.5,-2.12345]");
        assertThat(GeoJsonGeometry.roundCoordinates(null)).isNull();
    }
}
