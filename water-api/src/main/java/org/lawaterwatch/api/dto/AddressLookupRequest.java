package org.lawaterwatch.api.dto;

//JSON body for POST /api/address, kept out of the URL so addresses don't land in logs or browser history
public record AddressLookupRequest(String address) {
}
