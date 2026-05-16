package com.materials.quoting.loader;

import com.materials.quoting.model.QuoteRejectionReason;
import com.materials.quoting.repository.QuoteRejectionReasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the quote_rejection_reasons table with the standard rejection reasons
 * on application startup. Idempotent – skipped if data already exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)  // runs after BatteryDataLoader (which has no @Order, defaults to Ordered.LOWEST_PRECEDENCE)
public class RejectionReasonLoader implements CommandLineRunner {

    private static final List<String> DEFAULT_REASONS = List.of(
            "Price too low",
            "Found better offer elsewhere",
            "No longer need pickup",
            "Information was incorrect",
            "Other"
    );

    private final QuoteRejectionReasonRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Rejection reasons already loaded – skipping seed.");
            return;
        }

        List<QuoteRejectionReason> reasons = DEFAULT_REASONS.stream()
                .map(name -> QuoteRejectionReason.builder()
                        .reasonName(name)
                        .activeFl(true)
                        .build())
                .toList();

        repository.saveAll(reasons);
        log.info("Seeded {} rejection reasons into quote_rejection_reasons.", reasons.size());
    }
}

