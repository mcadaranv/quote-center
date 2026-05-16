package com.materials.quoting.service;

import com.materials.quoting.model.QuoteRejection;

import java.util.List;
import java.util.Optional;

public interface QuoteRejectionService {

    QuoteRejection create(QuoteRejection quoteRejection);

    Optional<QuoteRejection> findById(Long id);

    List<QuoteRejection> findAll();

    List<QuoteRejection> findByQuoteId(Long quoteId);

    List<QuoteRejection> findByReasonId(Long reasonId);

    QuoteRejection update(Long id, QuoteRejection updated);

    void delete(Long id);
}

