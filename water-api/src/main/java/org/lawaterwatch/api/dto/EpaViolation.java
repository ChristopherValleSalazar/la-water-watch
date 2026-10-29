package org.lawaterwatch.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//One row of EPA SDWIS /VIOLATION/PWSID/{pwsid}/JSON, the response is a top level array
//Dates arrive as "2023-01-01 00:00:00" and are parsed in the loader
@JsonIgnoreProperties(ignoreUnknown = true)
public record EpaViolation(
        @JsonProperty("violation_id") String violationId,
        @JsonProperty("pwsid") String pwsid,
        @JsonProperty("violation_code") String violationCode,
        @JsonProperty("violation_category_code") String violationCategoryCode,
        @JsonProperty("is_health_based_ind") String isHealthBasedInd,
        @JsonProperty("contaminant_code") String contaminantCode,
        @JsonProperty("compliance_status_code") String complianceStatusCode,
        @JsonProperty("compl_per_begin_date") String complPerBeginDate,
        @JsonProperty("compl_per_end_date") String complPerEndDate,
        @JsonProperty("rtc_date") String rtcDate,
        @JsonProperty("public_notification_tier") Integer publicNotificationTier
) {}
