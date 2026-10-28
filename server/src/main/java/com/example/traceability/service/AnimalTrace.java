package com.example.traceability.service;

import java.time.Instant;
import java.util.List;

public record AnimalTrace(int registrationNumber, String species, String origin, double weight,
                          Instant arrivalDateTime, List<TracedPart> parts)
{
}
