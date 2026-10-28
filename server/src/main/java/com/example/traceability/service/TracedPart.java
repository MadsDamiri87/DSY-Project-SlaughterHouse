package com.example.traceability.service;

import java.time.Instant;
import java.util.List;

public record TracedPart(String partId, String type, double weight,
                         Instant registrationDateTime, String trayId, List<String> productIds)
{
}
