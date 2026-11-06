package com.skr.erp.util;

import com.skr.erp.repository.InventoryBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchNumberGenerator {
    private final InventoryBatchRepository inventoryBatchRepository;

    // DB sequence drives the number, but we reconcile with real data so a lagging
    // sequence (fresh import, restore, manual/seed insert) can never cause a duplicate.
    public String generate() {
        long sequence = inventoryBatchRepository.getNextBatchSequence();
        long maxInData = inventoryBatchRepository.getMaxBatchNumberValue();
        long chosen = Math.max(sequence, maxInData + 1);
        if (chosen > sequence) {
            inventoryBatchRepository.resetBatchSequence(chosen);
        }
        return String.format("BT%06d", chosen);
    }
}