package com.example.logintegrity.controller;

import com.example.logintegrity.model.SecurityLog;
import com.example.logintegrity.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class LogController {
    private final HashService hashes; private final SupabaseService db; private final AlgorandService chain;
    public LogController(HashService hashes, SupabaseService db, AlgorandService chain) { this.hashes=hashes; this.db=db; this.chain=chain; }
    @GetMapping("/health") public Map<String,Object> health() { return Map.of("application", "online", "supabaseConfigured", db.configured(), "algorand", chain.status()); }
    @GetMapping("/logs") public List<Map> logs(@RequestParam(defaultValue="100") int limit) { return db.list(limit); }
    @PostMapping("/logs") public ResponseEntity<?> create(@Valid @RequestBody LogInput input) {
        String id = UUID.randomUUID().toString(); Instant now=Instant.now();
        String severity = input.severity().toUpperCase(Locale.ROOT);
        String canonical = String.join("|", severity, input.source(), input.eventType(), input.message(),
                Objects.toString(input.actor(), ""), Objects.toString(input.ipAddress(), ""), now.toString(), id);
        String hash=hashes.sha256(canonical);
        try {
            AlgorandService.ChainReceipt receipt=chain.anchor(hash);
            SecurityLog log = new SecurityLog(id,severity,input.source(),input.eventType(),input.message(),input.actor(),input.ipAddress(),now,canonical,hash,receipt.transactionId(),receipt.confirmedRound(),receipt.status(),null);
            db.insert(log);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id",id,"sha256",hash,"transactionId",Objects.toString(receipt.transactionId(),""),"chainStatus",receipt.status(),"demoMode",chain.demoMode(),"createdAt",now.toString()));
        } catch (Exception e) { return ResponseEntity.status(502).body(Map.of("error", e.getMessage() == null ? "Unable to store security log" : e.getMessage())); }
    }
    @PostMapping("/verify/{id}") public ResponseEntity<?> verify(@PathVariable String id) {
        Optional<Map> found=db.find(id); if(found.isEmpty()) return ResponseEntity.notFound().build();
        Map row=found.get(); String tx=Objects.toString(row.get("transaction_id"),""); String expected=Objects.toString(row.get("sha256"),"");
        String canonical=Objects.toString(row.get("canonical_payload"),"");
        boolean databaseHashMatches=hashes.sha256(canonical).equals(expected);
        boolean blockchainMatches=databaseHashMatches && chain.verify(tx,expected);
        String status = chain.demoMode() && databaseHashMatches ? "DEMO_VERIFIED" : (blockchainMatches ? "VERIFIED" : "NOT_VERIFIED");
        return ResponseEntity.ok(Map.of("id",id,"transactionId",tx,"storedHash",expected,"databaseHashMatches",databaseHashMatches,
                "blockchainMatch",blockchainMatches,"demoMode",chain.demoMode(),"status",status));
    }
    public record LogInput(@NotBlank String severity, @NotBlank String source, @NotBlank String eventType, @NotBlank String message, String actor, String ipAddress) {}
}
