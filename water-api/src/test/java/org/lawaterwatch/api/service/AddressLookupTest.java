package org.lawaterwatch.api.service;

import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.client.CensusGeocodeClient;
import org.lawaterwatch.api.dto.AddressLookupResponse;
import org.lawaterwatch.api.dto.GeoCodeResponse;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.WaterSystemRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//Address to water system with a mocked geocoder and geometry cache
class AddressLookupTest {

    private static final String ADDRESS = "1572 Ricardo St, Los Angeles, CA, 90033, USA";

    private final CensusGeocodeClient geocodeClient = mock(CensusGeocodeClient.class);
    private final SystemGeometryCache geometryCache = mock(SystemGeometryCache.class);
    private final WaterSystemService service = new WaterSystemService(
            mock(WaterSystemRepository.class), mock(DataMetaRepository.class), mock(ViolationRepository.class),
            geocodeClient, geometryCache);

    //Census returns x as longitude and y as latitude
    private static GeoCodeResponse.AddressMatch match(double lng, double lat) {
        return new GeoCodeResponse.AddressMatch("1572 RICARDO ST, LOS ANGELES, CA, 90033",
                new GeoCodeResponse.Coordinates(lng, lat), new GeoCodeResponse.AddressComponents("90033"));
    }

    @Test
    void addressInsideASystemGetsItsPwsid() {
        when(geocodeClient.geocode(ADDRESS)).thenReturn(match(-118.197, 34.061));
        when(geometryCache.findPwsid(34.061, -118.197)).thenReturn("CA1910067");

        assertEquals(new AddressLookupResponse("1572 RICARDO ST, LOS ANGELES, CA, 90033", 34.061, -118.197,
                "CA1910067", true), service.addressLookup(ADDRESS));
    }

    @Test
    void addressOutsideEverySystemKeepsCoordinates() {
        when(geocodeClient.geocode(ADDRESS)).thenReturn(match(-117.9, 33.7));
        when(geometryCache.findPwsid(33.7, -117.9)).thenReturn(null);

        AddressLookupResponse result = service.addressLookup(ADDRESS);

        assertEquals(33.7, result.lat());
        assertEquals(-117.9, result.lng());
        assertNull(result.pwsid());
        assertFalse(result.inServiceArea());
    }

    @Test
    void unknownAddressIsAllNulls() {
        when(geocodeClient.geocode("asdf")).thenReturn(null);

        assertEquals(new AddressLookupResponse(null, null, null, null, false), service.addressLookup("asdf"));
    }
}
