package org.lawaterwatch.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "violation", indexes = {
        @Index(columnList = "pwsid"),
        @Index(columnList = "violationId")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Violation {

    //Internal surrogate key, EPA violation ids are only unique within one water system
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Raw EPA violation id
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

    public Violation(String violationId, String pwsid) {
        this.violationId = violationId;
        this.pwsid = pwsid;
    }
}
