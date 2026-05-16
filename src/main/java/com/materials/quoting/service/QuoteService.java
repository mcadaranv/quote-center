package com.materials.quoting.service;

import com.materials.quoting.model.QuoteDetail;
import com.materials.quoting.model.QuoteHeader;
import com.materials.quoting.model.QuoteStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuoteService {

    /**
     * Create a quote header together with its detail lines.
     * At least one detail must be present — a header without metals is rejected.
     */
    QuoteHeader create(QuoteHeader quoteHeader);

    Optional<QuoteHeader> findById(Long quoteId);

    List<QuoteHeader> findAll();

    List<QuoteHeader> findByStatus(QuoteStatus status);

    List<QuoteHeader> findByBatteryId(Long batteryId);

    List<QuoteHeader> findByDateRange(LocalDate from, LocalDate to);

    /** Update header-level fields only (status, date, battery, qty, notes). */
    QuoteHeader updateHeader(Long quoteId, QuoteHeader updated);

    /** Add a new detail line to an existing quote. */
    QuoteDetail addDetail(Long quoteId, QuoteDetail detail);

    /** Update a single detail line in place. */
    QuoteDetail updateDetail(Long quoteId, Long detailId, QuoteDetail updated);

    /** Remove a single detail line. Throws if it would leave the quote with no details. */
    void removeDetail(Long quoteId, Long detailId);

    /** Delete the entire quote and all its detail lines. */
    void delete(Long quoteId);
}

