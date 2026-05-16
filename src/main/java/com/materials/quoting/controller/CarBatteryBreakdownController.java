package com.materials.quoting.controller;

import com.materials.quoting.model.CarBatteryBreakdown;
import com.materials.quoting.model.CarSearch;
import com.materials.quoting.service.CarBatteryBreakdownService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/car-battery-breakdowns")
@RequiredArgsConstructor
public class CarBatteryBreakdownController {

    private final CarBatteryBreakdownService service;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CarBatteryBreakdown> create(@RequestBody CarBatteryBreakdown body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * Search endpoint. make and model are required when any search param is provided.
     * Optional: trim narrows by variant; year filters to records whose model year
     * range covers the given year.
     *
     * <ul>
     *   <li>No params              → returns all records</li>
     *   <li>make + model           → all versions of that model</li>
     *   <li>make + model + trim    → all years for that trim</li>
     *   <li>make + model + year    → records matching that year</li>
     *   <li>make + model + trim + year → most specific lookup</li>
     * </ul>
     */
    @GetMapping
    public List<CarBatteryBreakdown> getAll(
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String trim,
            @RequestParam(required = false) Integer year) {

        if (make == null && model == null && trim == null && year == null) {
            return service.findAll();
        }
        return service.findBySearch(new CarSearch(make, model, trim, year));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarBatteryBreakdown> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<CarBatteryBreakdown> update(@PathVariable Long id,
                                                      @RequestBody CarBatteryBreakdown body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
