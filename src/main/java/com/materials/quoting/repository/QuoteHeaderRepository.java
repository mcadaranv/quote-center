package com.materials.quoting.repository;

import com.materials.quoting.model.QuoteHeader;
import com.materials.quoting.model.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteHeaderRepository extends JpaRepository<QuoteHeader, Long> {

    @Query("SELECT DISTINCT qh FROM QuoteHeader qh JOIN FETCH qh.battery LEFT JOIN FETCH qh.details")
    List<QuoteHeader> findAll();

    @Query("SELECT DISTINCT qh FROM QuoteHeader qh JOIN FETCH qh.battery LEFT JOIN FETCH qh.details WHERE qh.quoteId = :id")
    Optional<QuoteHeader> findById(@Param("id") Long id);

    @Query("SELECT DISTINCT qh FROM QuoteHeader qh JOIN FETCH qh.battery LEFT JOIN FETCH qh.details WHERE qh.quoteStatus = :status")
    List<QuoteHeader> findByQuoteStatus(@Param("status") QuoteStatus status);

    @Query("SELECT DISTINCT qh FROM QuoteHeader qh JOIN FETCH qh.battery LEFT JOIN FETCH qh.details " +
           "WHERE qh.submittedDate BETWEEN :from AND :to")
    List<QuoteHeader> findBySubmittedDateBetween(@Param("from") LocalDate from,
                                                 @Param("to") LocalDate to);

    @Query("SELECT DISTINCT qh FROM QuoteHeader qh JOIN FETCH qh.battery b LEFT JOIN FETCH qh.details WHERE b.id = :batteryId")
    List<QuoteHeader> findByBatteryId(@Param("batteryId") Long batteryId);
}
