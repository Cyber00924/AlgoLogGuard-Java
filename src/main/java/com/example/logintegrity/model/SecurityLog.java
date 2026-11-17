package com.example.logintegrity.model;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record SecurityLog(String id, @NotBlank String severity, @NotBlank String source,
                          @NotBlank String eventType, @NotBlank String message,
                          String actor, String ipAddress, Instant createdAt,
                          String canonicalPayload, String sha256, String transactionId, Long confirmedRound,
                          String chainStatus, Boolean integrityValid) {}
