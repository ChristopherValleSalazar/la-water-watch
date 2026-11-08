package org.lawaterwatch.api.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.lawaterwatch.api.dto.*;
import org.lawaterwatch.api.service.MapResponseCache;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

//Endpoints the frontend team builds against
@RestController
@RequestMapping("/api")
public class WaterController {

    private static final String MAP_EXAMPLE = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [
                      [
                        [-118.47149, 34.00177],
                        [-118.47132, 34.00185],
                        [-118.46925, 34.00298],
                        [-118.47149, 34.00177]
                      ]
                    ]
                  },
                  "properties": {
                    "pwsid": "CA1910146",
                    "name": "SANTA MONICA-CITY, WATER DIVISION",
                    "waterScore": 67,
                    "burdenScore": 36,
                    "labelLat": 34.02320449967911,
                    "labelLng": -118.48192445631416,
                    "population": 90729
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "MultiPolygon",
                    "coordinates": [
                      [
                        [
                          [-118.50785, 34.03739],
                          [-118.50671, 34.03695],
                          [-118.50569, 34.03893],
                          [-118.50785, 34.03739]
                        ]
                      ],
                      [
                        [
                          [-118.24005, 33.7634],
                          [-118.24012, 33.76403],
                          [-118.24023, 33.76512],
                          [-118.24005, 33.7634]
                        ]
                      ]
                    ]
                  },
                  "properties": {
                    "pwsid": "CA1910067",
                    "name": "LOS ANGELES-CITY, DEPT. OF WATER & POWER",
                    "waterScore": 50,
                    "burdenScore": 68,
                    "labelLat": 34.02041600078531,
                    "labelLng": -118.28770702198409,
                    "population": 3875566
                  }
                }
              ]
            }
            """;

    private final WaterSystemService waterSystemService;
    private final MapResponseCache mapResponseCache;

    public WaterController(WaterSystemService waterSystemService, MapResponseCache mapResponseCache) {
        this.waterSystemService = waterSystemService;
        this.mapResponseCache = mapResponseCache;
    }

    @GetMapping("/meta")
    public MetaResponse meta() {
        return waterSystemService.meta();
    }

    //Prebuilt bytes from MapResponseCache, gzipped when the browser accepts it (all do), the browser unzips before fetch().json()
    //ETag lets a repeat visit get a 304 with no body instead of the whole map again
    //Returns raw bytes, so Swagger needs to be told the real shape of the JSON
    //The example is a real response trimmed to 2 of the 11 features, each ring cut to 4 points
    @ApiResponse(responseCode = "200",
            description = "GeoJSON FeatureCollection, one feature per water system. Geometry is a Polygon or a MultiPolygon, "
                    + "coordinates are [lng, lat]: pass the response straight to Leaflet's L.geoJSON",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SystemMapResponse.class),
                    examples = @ExampleObject(name = "Trimmed real response", value = MAP_EXAMPLE)))
    @GetMapping("/systems/map")
    public ResponseEntity<byte[]> map(@RequestHeader(value = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncoding,
                                      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        MapResponseCache.Body body = mapResponseCache.get();
        boolean gzip = acceptEncoding != null && acceptEncoding.contains("gzip");
        String etag = gzip ? body.gzipEtag() : body.etag();

        boolean unchanged = ifNoneMatch != null && ifNoneMatch.contains("\"" + etag + "\"");

        var response = ResponseEntity.status(unchanged ? HttpStatus.NOT_MODIFIED : HttpStatus.OK)
                .eTag(etag)
                //no-cache = keep it but check the ETag first, so a data reload shows up on the next visit
                .cacheControl(CacheControl.noCache())
                .varyBy(HttpHeaders.ACCEPT_ENCODING);
        if (unchanged) {
            return response.build();
        }
        response.contentType(MediaType.APPLICATION_JSON);
        if (gzip) {
            response.header(HttpHeaders.CONTENT_ENCODING, "gzip");
            return response.body(body.gzip());
        }
        return response.body(body.json());
    }

    @GetMapping("/systems/info-panel/{pwsid}")
    public SystemInfoPanel infoPanel(@PathVariable String pwsid) {
       return waterSystemService.infoPanel(pwsid);
    }

    @GetMapping("/systems/avg-score")
    public AvgScoreResponse avgScore() {
        return waterSystemService.avgScore();
    }

    @GetMapping("/systems/leaderboard")
    public List<LeaderboardRow> leaderboard(@RequestParam(required = false, defaultValue = "NONE") Sort sort) {
        return waterSystemService.leaderboard(sort);
    }

    //POST so the address travels in the body, not the URL, and stays out of access logs
    @PostMapping("/address")
    public AddressLookupResponse addressLookup(@RequestBody AddressLookupRequest request) {
        if (request.address() == null || request.address().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "address is required");
        }
        return waterSystemService.addressLookup(request.address());
    }
}
