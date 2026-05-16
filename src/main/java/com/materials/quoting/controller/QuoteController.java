package com.materials.quoting.controller;

import com.materials.quoting.client.metals.Metal;
import com.materials.quoting.client.metals.MetalsPricingClient;
import com.materials.quoting.model.QuoteDetail;
import com.materials.quoting.model.QuoteHeader;
import com.materials.quoting.model.QuoteStatus;
import com.materials.quoting.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService service;
    private final MetalsPricingClient metalsPricingClient;

    // ── METALS PRICING HEALTH ──────────────────────────────────────────────────

    /**
     * Probes the MetalsPricing service by fetching the price of one metal.
     * Returns 200 {"status":"UP"} if reachable, 503 {"status":"DOWN"} otherwise.
     */
    @GetMapping("/metals-pricing/health")
    public ResponseEntity<Map<String, String>> metalsPricingHealth() {
        Map<String, String> body = new LinkedHashMap<>();
        try {
            metalsPricingClient.getPrice(Metal.NICKEL);
            body.put("status", "UP");
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            body.put("status", "DOWN");
            body.put("reason", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    // ── QUOTE (HEADER + DETAILS) ───────────────────────────────────────────────

    /** Create a quote. Body must include at least one detail line under "details". */
    @PostMapping
    public ResponseEntity<QuoteHeader> create(@RequestBody QuoteHeader body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @GetMapping
    public List<QuoteHeader> getAll(
            @RequestParam(required = false) QuoteStatus status,
            @RequestParam(required = false) Long batteryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (status != null)             return service.findByStatus(status);
        if (batteryId != null)          return service.findByBatteryId(batteryId);
        if (from != null && to != null) return service.findByDateRange(from, to);
        return service.findAll();
    }

    @GetMapping("/{quoteId}")
    public ResponseEntity<QuoteHeader> getById(@PathVariable Long quoteId) {
        return service.findById(quoteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Update header-level fields only (status, date, battery, qty, notes). */
    @PutMapping("/{quoteId}")
    public ResponseEntity<QuoteHeader> updateHeader(@PathVariable Long quoteId,
                                                    @RequestBody QuoteHeader body) {
        return ResponseEntity.ok(service.updateHeader(quoteId, body));
    }

    /** Delete the entire quote and all its detail lines. */
    @DeleteMapping("/{quoteId}")
    public ResponseEntity<Void> delete(@PathVariable Long quoteId) {
        service.delete(quoteId);
        return ResponseEntity.noContent().build();
    }

    // ── DETAIL LINES ──────────────────────────────────────────────────────────

    /** Add a new metal detail line to an existing quote. */
    @PostMapping("/{quoteId}/details")
    public ResponseEntity<QuoteDetail> addDetail(@PathVariable Long quoteId,
                                                 @RequestBody QuoteDetail body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addDetail(quoteId, body));
    }

    /** Update a single metal detail line. */
    @PutMapping("/{quoteId}/details/{detailId}")
    public ResponseEntity<QuoteDetail> updateDetail(@PathVariable Long quoteId,
                                                    @PathVariable Long detailId,
                                                    @RequestBody QuoteDetail body) {
        return ResponseEntity.ok(service.updateDetail(quoteId, detailId, body));
    }

    /** Remove a detail line. The quote must retain at least one detail. */
    @DeleteMapping("/{quoteId}/details/{detailId}")
    public ResponseEntity<Void> removeDetail(@PathVariable Long quoteId,
                                             @PathVariable Long detailId) {
        service.removeDetail(quoteId, detailId);
        return ResponseEntity.noContent().build();
    }
}
