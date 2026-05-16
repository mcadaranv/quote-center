package com.materials.quoting.controller;

import com.materials.quoting.model.QuoteRejection;
import com.materials.quoting.service.QuoteRejectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quote-rejections")
@RequiredArgsConstructor
public class QuoteRejectionController {

    private final QuoteRejectionService service;

    @PostMapping
    public ResponseEntity<QuoteRejection> create(@RequestBody QuoteRejection body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @GetMapping
    public List<QuoteRejection> getAll(
            @RequestParam(required = false) Long quoteId,
            @RequestParam(required = false) Long reasonId) {

        if (quoteId != null) return service.findByQuoteId(quoteId);
        if (reasonId != null) return service.findByReasonId(reasonId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteRejection> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuoteRejection> update(@PathVariable Long id,
                                                 @RequestBody QuoteRejection body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
