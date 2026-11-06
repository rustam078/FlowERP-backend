package com.skr.erp.qr.dto;

import lombok.Builder;
import lombok.Getter;

/** Cheap label counts for a batch so the labels drawer never loads every unit. */
@Getter
@Builder
public class BatchLabelSummary {

    private String batchNumber;
    private int received;          // batch pieces (for display)
    private int stock;             // sellable stock (quantityAvailable) — the labelling ceiling
    private long totalLabelled;    // all labels ever created for the batch
    private long availableLabels;  // labels still AVAILABLE (unsold) — basis for "remaining" and print
    private long sold;
    private long voidCount;
    private long remaining;        // unsold pieces not yet labelled = stock - availableLabels
}
