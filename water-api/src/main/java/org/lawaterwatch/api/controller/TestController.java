package org.lawaterwatch.api.controller;

import org.lawaterwatch.api.dto.GeoCodeResponse;
import org.lawaterwatch.api.service.AddressConverterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    private final AddressConverterService addressConverterService;

    public TestController(AddressConverterService addressConverterService) {
        this.addressConverterService = addressConverterService;
    }


    @GetMapping("/fullAddress")
    public ResponseEntity<GeoCodeResponse> addressConverter(@RequestParam String address) {
        return new ResponseEntity<>(addressConverterService.addressConverter(address), HttpStatus.OK);
    }

//    @GetMapping("/zipOnly")
//    public ResponseEntity<GeoCodeResponse> zipToWaterSystem(@RequestParam String zip) {
//
//    }


}