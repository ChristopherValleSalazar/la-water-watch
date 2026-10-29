package org.lawaterwatch.api.controller;

import org.lawaterwatch.api.dto.MetaResponse;
import org.lawaterwatch.api.dto.SystemDetailResponse;
import org.lawaterwatch.api.dto.SystemMapResponse;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Endpoints the frontend team builds against
@RestController
@RequestMapping("/api")
public class WaterController {

    private final WaterSystemService waterSystemService;

    public WaterController(WaterSystemService waterSystemService) {
        this.waterSystemService = waterSystemService;
    }

    @GetMapping("/meta")
    public MetaResponse meta() {
        return waterSystemService.meta();
    }

    @GetMapping("/systems/map")
    public SystemMapResponse map() {
        return waterSystemService.map();
    }

    @GetMapping("/systems/{pwsid}")
    public SystemDetailResponse detail(@PathVariable String pwsid) {
        return waterSystemService.detail(pwsid);
    }
}
