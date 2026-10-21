package org.lawaterwatch.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.List;

//SABL service area boundaries query, f=geojson
@JsonIgnoreProperties(ignoreUnknown = true)
public record SablFeatureCollection(List<Feature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(JsonNode geometry, Properties properties) {}

    //SABL pads strings with spaces ("CA  ", "800-342-5397      "), trimmed once here
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            @JsonProperty("SABL_PWSID") String pwsid,
            @JsonProperty("WATER_SYSTEM_NAME") String waterSystemName,
            @JsonProperty("BOUNDARY_TYPE") String boundaryType,
            @JsonProperty("POPULATION") Integer population,
            @JsonProperty("SERVICE_CONNECTIONS") Integer serviceConnections,
            @JsonProperty("COUNTY") String county,
            @JsonProperty("REGULATING_AGENCY") String regulatingAgency,
            @JsonProperty("AC_PHONE_NUMBER") String phone
    ) {
        public Properties {
            pwsid = trim(pwsid);
            waterSystemName = trim(waterSystemName);
            boundaryType = trim(boundaryType);
            county = trim(county);
            regulatingAgency = trim(regulatingAgency);
            phone = trim(phone);
        }

        //Blank becomes null, SABL uses " " for empty fields
        private static String trim(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
