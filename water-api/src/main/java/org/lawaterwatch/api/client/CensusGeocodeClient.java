package org.lawaterwatch.api.client;

import org.lawaterwatch.api.dto.GeoCodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

//Census Bureau geocoder, one line address to lng/lat
@Component
public class CensusGeocodeClient {

    //A user is waiting on the address search: long enough for a slow Census day, short enough not to hang
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CensusGeocodeClient(@Value("${app.census.url}") String url, ObjectMapper objectMapper) {
        this.restClient = RestClients.create(url, READ_TIMEOUT);
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
            //We're a gateway to Census here: its failure is a 502, its timeout a 504 (GlobalExceptionHandler formats both)
            if (e instanceof ResourceAccessException && e.getCause() instanceof HttpTimeoutException) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                        "The address service (U.S. Census geocoder) did not answer within "
                                + READ_TIMEOUT.toSeconds() + " s, please try again");
            }
            String status = e instanceof RestClientResponseException response
                    ? " (it answered " + response.getStatusCode().value() + ")" : "";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The address service (U.S. Census geocoder) is unavailable" + status + ", please try again later");
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
