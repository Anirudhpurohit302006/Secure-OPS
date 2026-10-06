package com.secureops.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.secureops.dto.Dtos.ActionResult;
import com.secureops.model.Models.Machine;
import com.secureops.repository.MachineRepository;
import com.secureops.service.CollectionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CollectionSchedulerTest {

    private static Machine machine(int id, String name, boolean enabled) {
        return new Machine(id, name, null, "10.0.0." + id, "ani", 22, enabled, "UNKNOWN", null, null, null, null, null);
    }

    @Test
    void collectsEnabledTargetsSequentiallyAndSurvivesFailures() {
        MachineRepository repo = mock(MachineRepository.class);
        CollectionService collection = mock(CollectionService.class);
        Machine a = machine(1, "RHEL-01", true);
        Machine b = machine(2, "RHEL-02", true);
        when(repo.findEnabled()).thenReturn(List.of(a, b));
        when(collection.collect(a)).thenThrow(new RuntimeException("boom"));
        when(collection.collect(b)).thenReturn(new ActionResult(true, "ok", 1));

        new CollectionScheduler(repo, collection).runCycle();

        InOrder order = inOrder(collection);
        order.verify(collection).collect(a);
        order.verify(collection).collect(b);   // second target still collected after the first crashed
    }

    @Test
    void doesNothingWhenNoTargetIsEnabled() {
        MachineRepository repo = mock(MachineRepository.class);
        CollectionService collection = mock(CollectionService.class);
        when(repo.findEnabled()).thenReturn(List.of());
        new CollectionScheduler(repo, collection).runCycle();
        verify(collection, never()).collect(any());
    }
}
