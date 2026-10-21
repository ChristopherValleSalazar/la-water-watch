package org.lawaterwatch.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "violation", indexes = @Index(columnList = "pwsid"))
@Data
@NoArgsConstructor
public class Violation {

    @Id
    private String violationId;

    private String pwsid;

    //e.g. TT, OTHER, etc
    private String violationCategoryCode;

    private String isHealthBasedInd;

    //Where going download csv lookup table on the EPA website, convert code to name through in memory hashmap @Component, then
    //call that before sending to frontend, now they have code and name
    private String contaminantCode;

    private String complianceStatusCode;
    private LocalDate complPerBeginDate;
    private LocalDate complPerEndDate;
    private LocalDate rtcDate;
    private String publicNotificationTier;
}
