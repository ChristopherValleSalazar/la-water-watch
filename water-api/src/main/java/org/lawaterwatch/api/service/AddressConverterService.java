package org.lawaterwatch.api.service;

import org.lawaterwatch.api.dto.GeoCodeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AddressConverterService {
    private final RestClient gepCodeClient;

    public AddressConverterService() {
         gepCodeClient = RestClient.builder()
                .baseUrl("https://geocoding.geo.census.gov/geocoder")
                .build();
    }

    public GeoCodeResponse addressConverter(String address) {


        GeoCodeResponse response = gepCodeClient.get()
                .uri(uri -> uri
                        .path("/{returntype}/{searchtype}")
                        .queryParam("address", address)
                        .queryParam("benchmark", "Public_AR_Current")
                        .queryParam("format", "json")
                        .build("locations", "onelineaddress"))
                .retrieve()
                .body(GeoCodeResponse.class);

        System.out.println(response.result().addressMatches()
                .get(0).addressComponents());

        return response;
    }





}
