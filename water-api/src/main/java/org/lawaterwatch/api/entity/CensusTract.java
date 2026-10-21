package org.lawaterwatch.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "census_tract", indexes = @Index(columnList = "pwsid"))
@Data
@NoArgsConstructor
public class CensusTract {

    //11 character, zero padded tract id , e.g. 06037101122
    @Id
    @Column(length = 11)
    private String tractId;

    // Water system this tract falls in, assigned by point-in-polygon during load
    @Column(length = 10, nullable = false)
    private String pwsid;

    private String city;          // AppoxLoc
    private Integer population;

    // Water
    private Double drinkingWater;      // raw index, debugging only
    private Double drinkingWaterPctl;

    // Overall CalEnviroScreen
    private Double ciScorePctl;
    private Double pollutionPctl;
    private Double popCharPctl;

    // EJ context
    private Double povertyPctl;
    private Double asthmaPctl;
    private Double childLeadPctl;
    private Double toxReleasesPctl;
    private Double gwThreatsPctl;
    private Double cleanupSitesPctl;
    private Double airPm25Pctl;






}
