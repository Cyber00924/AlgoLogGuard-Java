package com.example.logintegrity.service;

import com.example.logintegrity.model.SecurityLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service
public class SupabaseService {
    private final RestClient client;
    private final boolean configured;
    public SupabaseService(@Value("${supabase.url:}") String url, @Value("${supabase.secret-key:}") String key) {
        configured = url != null && !url.isBlank() && key != null && !key.isBlank();
        client = configured ? RestClient.builder().baseUrl(url + "/rest/v1").defaultHeader("apikey", key)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build() : null;
    }
    public boolean configured() { return configured; }
    public void insert(SecurityLog log) {
        if (!configured) throw new IllegalStateException("Supabase is not configured. Set SUPABASE_URL and SUPABASE_SECRET_KEY.");
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("id", log.id()); row.put("severity", log.severity()); row.put("source", log.source()); row.put("event_type", log.eventType());
        row.put("message", log.message()); row.put("actor", log.actor()); row.put("ip_address", log.ipAddress() == null || log.ipAddress().isBlank() ? null : log.ipAddress()); row.put("created_at", log.createdAt().toString());
        row.put("canonical_payload", log.canonicalPayload()); row.put("sha256", log.sha256()); row.put("transaction_id", log.transactionId()); row.put("confirmed_round", log.confirmedRound());
        row.put("chain_status", log.chainStatus()); row.put("integrity_valid", log.integrityValid());
        client.post().uri("/security_logs").header("Prefer", "return=minimal").body(row).retrieve().toBodilessEntity();
    }
    public List<Map> list(int limit) {
        if (!configured) return List.of();
        return client.get().uri(uri -> uri.path("/security_logs").queryParam("select", "*").queryParam("order", "created_at.desc").queryParam("limit", Math.min(Math.max(limit,1),200)).build())
                .retrieve().body(List.class);
    }
    public Optional<Map> find(String id) {
        if (!configured) return Optional.empty();
        List<Map> rows = client.get().uri(uri -> uri.path("/security_logs").queryParam("select", "*").queryParam("id", "eq."+id).queryParam("limit", 1).build()).retrieve().body(List.class);
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
