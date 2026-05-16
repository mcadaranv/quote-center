package com.materials.quoting.service;

import com.materials.quoting.model.QuoteRejectionReason;

import java.util.List;
import java.util.Optional;

public interface QuoteRejectionReasonService {

    QuoteRejectionReason create(QuoteRejectionReason reason);

    Optional<QuoteRejectionReason> findById(Long reasonId);

    List<QuoteRejectionReason> findAll();

    List<QuoteRejectionReason> findByActive(boolean active);

    List<QuoteRejectionReason> findByKeyword(String keyword);

    QuoteRejectionReason update(Long reasonId, QuoteRejectionReason updated);

    void delete(Long reasonId);
}

