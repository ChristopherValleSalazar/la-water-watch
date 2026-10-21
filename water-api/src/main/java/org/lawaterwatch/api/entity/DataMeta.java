package org.lawaterwatch.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "data-meta")
@Data
@NoArgsConstructor
public class DataMeta {

    @Id
    private final Integer id = 1;

    @Column(name = "time_stamp")
    private Instant timeStamp;

    private Integer version;



}
