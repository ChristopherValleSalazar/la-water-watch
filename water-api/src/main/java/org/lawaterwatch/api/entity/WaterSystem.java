package org.lawaterwatch.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "water_system")
@Data
public class WaterSystem {

    @Id
    private String pwsid;

    private String name;
    private String county;
    private Integer population;
    private String serviceConnections;
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
    //0 - 100, population weighted CalEnviroScreen overall percentile, higher is more burderned
    private Integer burderScore;

    private Double asthmaPctl;
    private Double povertyPctl;
    private Double leadPctl;
    private Double toxReleasesPctl;
    private Double gwThreatsPctl;
    private Double cleanupsPctl;
    private Double pm25Pctl;

    private Integer tractCount;
    private Long tractPopulation;
}

