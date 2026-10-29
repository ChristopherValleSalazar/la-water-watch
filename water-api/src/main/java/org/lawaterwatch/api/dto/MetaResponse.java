package org.lawaterwatch.api.dto;

import java.time.Instant;

//GET /api/meta, lastRefreshed is null before the first successful load
public record MetaResponse(Instant lastRefreshed, String source) {}
