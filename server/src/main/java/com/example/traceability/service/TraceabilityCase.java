package com.example.traceability.service;

import java.time.LocalDateTime;

public record TraceabilityCase(String subject, LocalDateTime requestDate, CaseStatus status)
{
}
