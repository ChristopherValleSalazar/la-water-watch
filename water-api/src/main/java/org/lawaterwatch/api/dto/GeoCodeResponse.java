package org.lawaterwatch.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoCodeResponse(Result result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(List<AddressMatch> addressMatches) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressMatch(String matchedAddress, Coordinates coordinates,
                               AddressComponents addressComponents) {}

    public record Coordinates(double x, double y) {}

    public record AddressComponents(String zip) {}
}
