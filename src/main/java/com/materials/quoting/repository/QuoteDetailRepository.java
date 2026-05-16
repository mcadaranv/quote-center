package com.materials.quoting.repository;

import com.materials.quoting.model.QuoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteDetailRepository extends JpaRepository<QuoteDetail, Long> {

    /** Fetch all detail lines, joining quote header and its battery in one query. */
    @Query("SELECT qd FROM QuoteDetail qd JOIN FETCH qd.quote qh JOIN FETCH qh.battery")
    List<QuoteDetail> findAll();

    /** Fetch a single detail line by ID with full join. */
    @Query("SELECT qd FROM QuoteDetail qd JOIN FETCH qd.quote qh JOIN FETCH qh.battery WHERE qd.id = :id")
    Optional<QuoteDetail> findById(@Param("id") Long id);

    /** Fetch all detail lines belonging to a specific quote. */
    @Query("SELECT qd FROM QuoteDetail qd JOIN FETCH qd.quote qh JOIN FETCH qh.battery WHERE qh.quoteId = :quoteId")
    List<QuoteDetail> findByQuoteQuoteId(@Param("quoteId") Long quoteId);

    /** Fetch all detail lines for a given metal type (case-insensitive). */
    @Query("SELECT qd FROM QuoteDetail qd JOIN FETCH qd.quote qh JOIN FETCH qh.battery WHERE LOWER(qd.metal) = LOWER(:metal)")
    List<QuoteDetail> findByMetalIgnoreCase(@Param("metal") String metal);
}
