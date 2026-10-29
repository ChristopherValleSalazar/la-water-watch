package org.lawaterwatch.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "water_system")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaterSystem {

    @Id
    private String pwsid;

    private String name;
    private String county;
    private Integer population;
    private Integer serviceConnections;
    private String phone;
    private String regulatingAgency;

    //Represents the polygons from raw GeoJson object, lng/lat
    @Column(columnDefinition = "TEXT")
    private String geometryGeoJson;

    //Where the score circle is drawn
    private Double labelLat;
    private Double labelLng;

    //0 - 100, higher is cleaner
    private Integer waterScore;
    //0 - 100, population weighted CalEnviroScreen overall percentile, higher is more burdened
    private Integer burdenScore;

    private Double avgAsthmaPctl;
    private Double avgPovertyPctl;
    private Double avgChildLeadPctl;
    private Double avgToxReleasesPctl;
    private Double avgGwThreatsPctl;
    private Double avgCleanupSitesPctl;
    private Double avgAirPm25Pctl;

    private Integer tractCount;
    private Long tractPopulation;

    public WaterSystem(String pwsid) {
        this.pwsid = pwsid;
    }
}

