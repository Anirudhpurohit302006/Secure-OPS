package com.secureops.scheduler;

import com.secureops.model.Models.Machine;
import com.secureops.repository.MachineRepository;
import com.secureops.service.CollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs one sequential collection cycle every interval (default 30 min, max 45).
 * fixedDelay means the next cycle starts <interval> after the previous one FINISHED, so cycles never overlap.
 * Offline targets are retried only on the next cycle (no retry loops).
 */
@Component
@ConditionalOnProperty(name = "secureops.collection.enabled", havingValue = "true", matchIfMissing = true)
public class CollectionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CollectionScheduler.class);

    private final MachineRepository machines;
    private final CollectionService collection;

    public CollectionScheduler(MachineRepository machines, CollectionService collection) {
        this.machines = machines;
        this.collection = collection;
    }

    @Scheduled(initialDelayString = "#{${secureops.collection.initial-delay-seconds:20} * 1000}",
            fixedDelayString = "#{${secureops.collection.interval-minutes:30} * 60 * 1000}")
    public void scheduledCycle() { runCycle(); }

    /** Package-visible for tests. */
    public void runCycle() {
        log.info("Collection cycle started");
        for (Machine m : machines.findEnabled()) {
            try {
                var result = collection.collect(m);
                log.info("{}: {}", m.machineName(), result.success() ? "collected" : "failed - " + result.message());
            } catch (RuntimeException e) {
                log.error("Collection crashed for {}", m.machineName(), e);   // never stop the cycle
            }
        }
        log.info("Collection cycle finished");
    }
}
