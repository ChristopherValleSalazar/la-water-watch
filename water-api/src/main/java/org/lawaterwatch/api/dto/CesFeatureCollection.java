package org.lawaterwatch.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.List;

//CalEnviroScreen 5.0 draft layer query, f=geojson, one feature per 2020 census tract
@JsonIgnoreProperties(ignoreUnknown = true)
public record CesFeatureCollection(
        List<Feature> features,
        Boolean exceededTransferLimit,
        Meta properties
) {

    //Exactly the fields the app uses, a misspelled name makes ArcGIS reject the whole query
    public static final String OUT_FIELDS = String.join(",",
            "tract", "AppoxLoc", "Population", "DrinkingWater", "DrinkingWater_Pctl",
            "CIscore_Pctl", "Pollution_Pctl", "PopChar_Pctl", "Poverty_Pctl", "Asthma_Pctl",
            "ChildLead_Pctl", "ToxReleases_Pctl", "GWThreats_Pctl", "CleanupSites_Pctl", "AirPM25_Pctl");

    //In f=geojson the flag sits under "properties", check the top level too in case that changes
    public boolean moreAvailable() {
        return Boolean.TRUE.equals(exceededTransferLimit)
                || (properties != null && Boolean.TRUE.equals(properties.exceededTransferLimit()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(Boolean exceededTransferLimit) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(JsonNode geometry, Properties properties) {}

    //Percentiles: 0 is real data, null or negative means missing
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            @JsonProperty("tract") Long tract,
            @JsonProperty("AppoxLoc") String appoxLoc,
            @JsonProperty("Population") Integer population,
            @JsonProperty("DrinkingWater") Double drinkingWater,
            @JsonProperty("DrinkingWater_Pctl") Double drinkingWaterPctl,
            @JsonProperty("CIscore_Pctl") Double ciScorePctl,
            @JsonProperty("Pollution_Pctl") Double pollutionPctl,
            @JsonProperty("PopChar_Pctl") Double popCharPctl,
            @JsonProperty("Poverty_Pctl") Double povertyPctl,
            @JsonProperty("Asthma_Pctl") Double asthmaPctl,
            @JsonProperty("ChildLead_Pctl") Double childLeadPctl,
            @JsonProperty("ToxReleases_Pctl") Double toxReleasesPctl,
            @JsonProperty("GWThreats_Pctl") Double gwThreatsPctl,
            @JsonProperty("CleanupSites_Pctl") Double cleanupSitesPctl,
            @JsonProperty("AirPM25_Pctl") Double airPm25Pctl
    ) {
        //6037101122 -> "06037101122", the API drops the leading zero of the state code
        public String tractId() {
            return tract == null ? null : String.format("%011d", tract);
        }
    }
}
