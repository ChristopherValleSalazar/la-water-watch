package org.lawaterwatch.api.bootstrap;

import org.lawaterwatch.api.entity.CensusTract;
import org.lawaterwatch.api.entity.DataMeta;
import org.lawaterwatch.api.entity.Violation;
import org.lawaterwatch.api.entity.WaterSystem;
import org.lawaterwatch.api.repository.CensusTractRepository;
import org.lawaterwatch.api.repository.DataMetaRepository;
import org.lawaterwatch.api.repository.ViolationRepository;
import org.lawaterwatch.api.repository.WaterSystemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collection;

//Replaces all loaded data in one transaction, a failure rolls back and keeps the old data
@Component
public class DataWriter {

    private final WaterSystemRepository waterSystemRepository;
    private final CensusTractRepository censusTractRepository;
    private final ViolationRepository violationRepository;
    private final DataMetaRepository dataMetaRepository;
    private final TransactionTemplate transactionTemplate;

    public DataWriter(WaterSystemRepository waterSystemRepository,
                      CensusTractRepository censusTractRepository,
                      ViolationRepository violationRepository,
                      DataMetaRepository dataMetaRepository,
                      TransactionTemplate transactionTemplate) {
        this.waterSystemRepository = waterSystemRepository;
        this.censusTractRepository = censusTractRepository;
        this.violationRepository = violationRepository;
        this.dataMetaRepository = dataMetaRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public void replaceAll(Collection<WaterSystem> systems, Collection<CensusTract> tracts,
                           Collection<Violation> violations) {
        transactionTemplate.executeWithoutResult(status -> {
            violationRepository.deleteAllInBatch();
            censusTractRepository.deleteAllInBatch();
            waterSystemRepository.deleteAllInBatch();

            waterSystemRepository.saveAll(systems);
            censusTractRepository.saveAll(tracts);
            violationRepository.saveAll(violations);

            DataMeta meta = dataMetaRepository.findById(DataMeta.SINGLETON_ID).orElseGet(DataMeta::new);
            meta.setLastRefreshed(Instant.now());
            dataMetaRepository.save(meta);
        });
    }
}
