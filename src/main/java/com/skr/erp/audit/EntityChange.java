package com.skr.erp.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A row change seen by the Hibernate listener, held in memory until the request's audit row is written. */
record EntityChange(UUID portfolioId, String entityName, UUID entityId, String operation,
                    JsonNode before, JsonNode after, List<String> changedFields, LocalDateTime at) {
}
