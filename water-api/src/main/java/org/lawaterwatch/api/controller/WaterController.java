package org.lawaterwatch.api.controller;

import org.lawaterwatch.api.dto.Filter;
import org.lawaterwatch.api.dto.MetaResponse;
import org.lawaterwatch.api.dto.SystemMapResponse;
import org.lawaterwatch.api.dto.SystemInfoPanel;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.web.bind.annotation.*;

//Endpoints the frontend team builds against
@RestController
@RequestMapping("/api")
@CrossOrigin("http://127.0.0.1:3000/")
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

    @GetMapping("/systems/info-panel/{pwsid}")
    public SystemInfoPanel infoPanel(@PathVariable String pwsid) {
       return waterSystemService.infoPanel(pwsid);
    }

    @GetMapping("/systems/leaderboard/")
    public Void leaderboard(@RequestParam(required = false) Filter filter) {
        return waterSystemService.leaderboard(filter);
    }




}
