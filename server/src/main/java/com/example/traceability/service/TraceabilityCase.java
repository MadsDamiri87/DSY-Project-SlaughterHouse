package com.example.traceability.service;

import java.time.Instant;

public record TraceabilityCase(String subject, Instant requestDate, CaseStatus status)
{
}
