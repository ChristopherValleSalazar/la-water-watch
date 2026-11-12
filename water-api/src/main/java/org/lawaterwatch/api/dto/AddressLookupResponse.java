package org.lawaterwatch.api.dto;

//Where a searched address landed and which water system serves it
//Everything but inServiceArea is null when the geocoder can't place the address
//pwsid is null when the point is outside every tracked system, inServiceArea says the same thing explicitly
public record AddressLookupResponse(
        String matchedAddress,
        Double lat,
        Double lng,
        String pwsid,
        boolean inServiceArea
) {
    //Derives the flag from pwsid so the two can't disagree
    public AddressLookupResponse(String matchedAddress, Double lat, Double lng, String pwsid) {
        this(matchedAddress, lat, lng, pwsid, pwsid != null);
    }

    public static AddressLookupResponse notFound() {
        return new AddressLookupResponse(null, null, null, null);
    }
}
