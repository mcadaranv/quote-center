package com.materials.quoting.service;

import com.materials.quoting.model.QuoteRejectionReason;
import com.materials.quoting.repository.QuoteRejectionReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuoteRejectionReasonServiceImpl implements QuoteRejectionReasonService {

    private final QuoteRejectionReasonRepository repository;

    @Override
    public QuoteRejectionReason create(QuoteRejectionReason reason) {
        reason.setReasonId(null);
        return repository.save(reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuoteRejectionReason> findById(Long reasonId) {
        return repository.findById(reasonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRejectionReason> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRejectionReason> findByActive(boolean active) {
        return repository.findByActiveFl(active);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRejectionReason> findByKeyword(String keyword) {
        return repository.findByReasonNameContainingIgnoreCase(keyword);
    }

    @Override
    public QuoteRejectionReason update(Long reasonId, QuoteRejectionReason updated) {
        QuoteRejectionReason existing = repository.findById(reasonId)
                .orElseThrow(() -> new NoSuchElementException("QuoteRejectionReason not found with id: " + reasonId));

        existing.setReasonName(updated.getReasonName());
        existing.setActiveFl(updated.getActiveFl());

        return repository.save(existing);
    }

    @Override
    public void delete(Long reasonId) {
        QuoteRejectionReason entity = repository.findById(reasonId)
                .orElseThrow(() -> new NoSuchElementException("QuoteRejectionReason not found with id: " + reasonId));
        repository.delete(entity);
    }
}

