package com.materials.quoting.repository;

import com.materials.quoting.model.QuoteRejectionReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRejectionReasonRepository extends JpaRepository<QuoteRejectionReason, Long> {

    List<QuoteRejectionReason> findByActiveFl(boolean activeFl);

    List<QuoteRejectionReason> findByReasonNameContainingIgnoreCase(String keyword);
}

