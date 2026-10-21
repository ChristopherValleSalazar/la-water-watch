package org.lawaterwatch.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "data_meta")
@Data
@NoArgsConstructor
public class DataMeta {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id = SINGLETON_ID;

    private Instant lastRefreshed;
    private Integer version;
}
