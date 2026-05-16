package com.materials.quoting.repository;

import com.materials.quoting.model.QuoteRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRejectionRepository extends JpaRepository<QuoteRejection, Long> {

    /** Fetch all rejections with quote, battery and reason joined. */
    @Query("SELECT qr FROM QuoteRejection qr " +
           "JOIN FETCH qr.quote qh JOIN FETCH qh.battery LEFT JOIN FETCH qr.reason")
    List<QuoteRejection> findAll();

    /** Fetch a single rejection by ID with full join. */
    @Query("SELECT qr FROM QuoteRejection qr " +
           "JOIN FETCH qr.quote qh JOIN FETCH qh.battery LEFT JOIN FETCH qr.reason " +
           "WHERE qr.id = :id")
    Optional<QuoteRejection> findById(@Param("id") Long id);

    /** Fetch all rejections for a given quote. */
    @Query("SELECT qr FROM QuoteRejection qr " +
           "JOIN FETCH qr.quote qh JOIN FETCH qh.battery LEFT JOIN FETCH qr.reason " +
           "WHERE qh.quoteId = :quoteId")
    List<QuoteRejection> findByQuoteQuoteId(@Param("quoteId") Long quoteId);

    /** Fetch all rejections linked to a specific rejection reason. */
    @Query("SELECT qr FROM QuoteRejection qr " +
           "JOIN FETCH qr.quote qh JOIN FETCH qh.battery JOIN FETCH qr.reason r " +
           "WHERE r.reasonId = :reasonId")
    List<QuoteRejection> findByReasonReasonId(@Param("reasonId") Long reasonId);
}
