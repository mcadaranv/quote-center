package com.materials.quoting.controller;

import com.materials.quoting.model.QuoteRejectionReason;
import com.materials.quoting.service.QuoteRejectionReasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quote-rejection-reasons")
@RequiredArgsConstructor
public class QuoteRejectionReasonController {

    private final QuoteRejectionReasonService service;

    @PostMapping
    public ResponseEntity<QuoteRejectionReason> create(@RequestBody QuoteRejectionReason body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @GetMapping
    public List<QuoteRejectionReason> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword) {

        if (active != null) return service.findByActive(active);
        if (keyword != null) return service.findByKeyword(keyword);
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteRejectionReason> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuoteRejectionReason> update(@PathVariable Long id,
                                                       @RequestBody QuoteRejectionReason body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
