package org.lawaterwatch.api.client;

import org.lawaterwatch.api.dto.GeoCodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

//Census Bureau geocoder, one line address to lng/lat
@Component
public class CensusGeocodeClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CensusGeocodeClient(@Value("${app.census.url}") String url, ObjectMapper objectMapper) {
        this.restClient = RestClients.create(url);
        this.objectMapper = objectMapper;
    }

    //Best match for the address, null when the geocoder can't place it
    //Takes the full "street, city, CA, zip, USA" string the frontend's search box gives
    public GeoCodeResponse.AddressMatch geocode(String address) {
        String body;
        try {
            body = restClient.get()
                    .uri(uri -> uri
                            .path("/locations/onelineaddress")
                            .queryParam("address", "{address}")
                            .queryParam("benchmark", "Public_AR_Current")
                            .queryParam("format", "json")
                            .build(address))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            //Spring's message holds the full request URL, address included, so rethrow without it or the cause
            String status = e instanceof RestClientResponseException response
                    ? " with status " + response.getStatusCode().value() : "";
            throw new IllegalStateException("Census geocoder request failed" + status);
        }

        if (body == null || body.isBlank()) {
            return null;
        }
        GeoCodeResponse response = objectMapper.readValue(body, GeoCodeResponse.class);
        if (response.result() == null) {
            return null;
        }
        List<GeoCodeResponse.AddressMatch> matches = response.result().addressMatches();
        return matches == null || matches.isEmpty() ? null : matches.getFirst();
    }
}
