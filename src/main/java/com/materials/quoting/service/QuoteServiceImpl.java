package com.materials.quoting.service;

import com.materials.quoting.client.metals.Metal;
import com.materials.quoting.client.metals.MetalPriceResponse;
import com.materials.quoting.client.metals.MetalsPricingClient;
import com.materials.quoting.model.QuoteDetail;
import com.materials.quoting.model.QuoteHeader;
import com.materials.quoting.model.QuoteStatus;
import com.materials.quoting.repository.QuoteDetailRepository;
import com.materials.quoting.repository.QuoteHeaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuoteServiceImpl implements QuoteService {

    private final QuoteHeaderRepository headerRepository;
    private final QuoteDetailRepository detailRepository;
    private final MetalsPricingClient metalsPricingClient;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public QuoteHeader create(QuoteHeader quoteHeader) {
        if (quoteHeader.getDetails() == null || quoteHeader.getDetails().isEmpty()) {
            throw new IllegalArgumentException("A quote must have at least one metal detail line.");
        }
        quoteHeader.setQuoteId(null);

        List<String> unsupportedMetals = new ArrayList<>();

        for (QuoteDetail detail : quoteHeader.getDetails()) {
            detail.setId(null);
            detail.setQuote(quoteHeader);

            Optional<Metal> metalOpt = Metal.tryFromValue(detail.getMetal());

            if (metalOpt.isEmpty()) {
                // Unsupported metal — zero out pricing fields
                log.warn("Unsupported metal '{}' — setting pricePerAmount and valueInDollars to 0", detail.getMetal());
                detail.setPricePerAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                detail.setValueInDollars(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                unsupportedMetals.add(detail.getMetal());
            } else {
                // Fetch live spot price from the metals pricing API
                MetalPriceResponse priceResponse = metalsPricingClient.getPrice(metalOpt.get());
                BigDecimal pricePerAmount = BigDecimal.valueOf(priceResponse.price())
                        .setScale(4, RoundingMode.HALF_UP);
                detail.setPricePerAmount(pricePerAmount);

                log.debug("Fetched price for {}: {} {}/{}", metalOpt.get(), priceResponse.price(),
                        priceResponse.currency(), priceResponse.unit());

                // Calculate valueInDollars = amount * pricePerAmount, rounded to 2 dp (currency)
                if (detail.getAmount() != null) {
                    BigDecimal valueInDollars = detail.getAmount()
                            .multiply(pricePerAmount)
                            .setScale(2, RoundingMode.HALF_UP);
                    detail.setValueInDollars(valueInDollars);
                }
            }
        }

        // Append unsupported metal warning to notes
        if (!unsupportedMetals.isEmpty()) {
            String warning = "Unsupported metals (priced at $0): " + String.join(", ", unsupportedMetals);
            String existingNotes = quoteHeader.getNotes();
            quoteHeader.setNotes(existingNotes != null && !existingNotes.isBlank()
                    ? existingNotes + " | " + warning
                    : warning);
            log.warn("Quote created with unsupported metals: {}", unsupportedMetals);
        }

        return headerRepository.save(quoteHeader);
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<QuoteHeader> findById(Long quoteId) {
        return headerRepository.findById(quoteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteHeader> findAll() {
        return headerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteHeader> findByStatus(QuoteStatus status) {
        return headerRepository.findByQuoteStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteHeader> findByBatteryId(Long batteryId) {
        return headerRepository.findByBatteryId(batteryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteHeader> findByDateRange(LocalDate from, LocalDate to) {
        return headerRepository.findBySubmittedDateBetween(from, to);
    }

    // ── UPDATE HEADER ─────────────────────────────────────────────────────────

    @Override
    public QuoteHeader updateHeader(Long quoteId, QuoteHeader updated) {
        QuoteHeader existing = headerRepository.findById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Quote not found with id: " + quoteId));

        existing.setSubmittedDate(updated.getSubmittedDate());
        existing.setQuoteStatus(updated.getQuoteStatus());
        existing.setBattery(updated.getBattery());
        existing.setBatteryQty(updated.getBatteryQty());
        existing.setNotes(updated.getNotes());

        return headerRepository.save(existing);
    }

    // ── DETAIL OPERATIONS ─────────────────────────────────────────────────────

    @Override
    public QuoteDetail addDetail(Long quoteId, QuoteDetail detail) {
        QuoteHeader header = headerRepository.findById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Quote not found with id: " + quoteId));

        detail.setId(null);
        detail.setQuote(header);
        header.getDetails().add(detail);
        headerRepository.save(header);
        return detail;
    }

    @Override
    public QuoteDetail updateDetail(Long quoteId, Long detailId, QuoteDetail updated) {
        // Verify the quote exists
        headerRepository.findById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Quote not found with id: " + quoteId));

        QuoteDetail existing = detailRepository.findById(detailId)
                .orElseThrow(() -> new NoSuchElementException("Detail not found with id: " + detailId));

        if (!existing.getQuote().getQuoteId().equals(quoteId)) {
            throw new IllegalArgumentException("Detail " + detailId + " does not belong to quote " + quoteId);
        }

        existing.setMetal(updated.getMetal());
        existing.setAmount(updated.getAmount());
        existing.setPricePerAmount(updated.getPricePerAmount());
        existing.setValueInDollars(updated.getValueInDollars());

        return detailRepository.save(existing);
    }

    @Override
    public void removeDetail(Long quoteId, Long detailId) {
        QuoteHeader header = headerRepository.findById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Quote not found with id: " + quoteId));

        if (header.getDetails().size() <= 1) {
            throw new IllegalArgumentException("Cannot remove the last detail line — a quote must have at least one.");
        }

        QuoteDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new NoSuchElementException("Detail not found with id: " + detailId));

        if (!detail.getQuote().getQuoteId().equals(quoteId)) {
            throw new IllegalArgumentException("Detail " + detailId + " does not belong to quote " + quoteId);
        }

        header.getDetails().remove(detail);
        headerRepository.save(header);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long quoteId) {
        QuoteHeader header = headerRepository.findById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("Quote not found with id: " + quoteId));
        headerRepository.delete(header); // cascades to details
    }
}

