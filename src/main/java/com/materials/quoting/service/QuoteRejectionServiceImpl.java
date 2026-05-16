package com.materials.quoting.service;

import com.materials.quoting.model.QuoteRejection;
import com.materials.quoting.repository.QuoteRejectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuoteRejectionServiceImpl implements QuoteRejectionService {

    private final QuoteRejectionRepository repository;

    @Override
    public QuoteRejection create(QuoteRejection quoteRejection) {
        quoteRejection.setId(null);
        return repository.save(quoteRejection);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuoteRejection> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRejection> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRejection> findByQuoteId(Long quoteId) {
        return repository.findByQuoteQuoteId(quoteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRejection> findByReasonId(Long reasonId) {
        return repository.findByReasonReasonId(reasonId);
    }

    @Override
    public QuoteRejection update(Long id, QuoteRejection updated) {
        QuoteRejection existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("QuoteRejection not found with id: " + id));

        existing.setQuote(updated.getQuote());
        existing.setReason(updated.getReason());
        existing.setOtherValue(updated.getOtherValue());
        existing.setComments(updated.getComments());

        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        QuoteRejection entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("QuoteRejection not found with id: " + id));
        repository.delete(entity);
    }
}

