package org.lawaterwatch.api.controller;

import io.swagger.v3.oas.annotations.media.Content;
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
    @ApiResponse(responseCode = "200", description = "GeoJSON FeatureCollection, one feature per water system",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemMapResponse.class)))
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
