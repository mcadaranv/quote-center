package com.materials.quoting.loader;

import com.materials.quoting.model.CarBatteryBreakdown;
import com.materials.quoting.repository.CarBatteryBreakdownRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads battery-data.csv into the car_battery_breakdown table on application startup.
 * The loader is idempotent – if the table already contains data it skips the import.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class BatteryDataLoader implements CommandLineRunner {

    private final CarBatteryBreakdownRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() > 0) {
            log.info("Battery data already loaded – skipping CSV import.");
            return;
        }

        log.info("Loading battery data from battery-data.csv …");

        List<CarBatteryBreakdown> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource("battery-data.csv").getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] cols = parseCsvLine(line);
                if (cols.length < 13) {
                    log.warn("Skipping malformed line: {}", line);
                    continue;
                }

                CarBatteryBreakdown record = CarBatteryBreakdown.builder()
                        .make(nullIfBlank(cols[0]))
                        .model(nullIfBlank(cols[1]))
                        .trim(nullIfBlank(cols[2]))
                        .propulsionType(nullIfBlank(cols[3]))
                        .batteryCapacityKwh(parseCapacity(cols[4]))
                        .modelYearStart(parseInteger(cols[5]))
                        .modelYearEnd(parseInteger(cols[6]))
                        .packWeightKg(parseDouble(cols[7]))
                        .niWeightKg(parseDouble(cols[8]))
                        .coWeightKg(parseDouble(cols[9]))
                        .liWeightKg(parseDouble(cols[10]))
                        .cuWeightKg(parseDouble(cols[11]))
                        .alWeightKg(parseDouble(cols[12]))
                        .build();

                records.add(record);
            }
        }

        repository.saveAll(records);
        log.info("Loaded {} battery records into car_battery_breakdown.", records.size());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Simple CSV parser that respects double-quoted fields containing commas.
     */
    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** Parses values like "21.3 kWh" → 21.3 */
    private Double parseCapacity(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String numeric = raw.replace("kWh", "").trim();
        try {
            return Double.parseDouble(numeric);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse battery capacity: '{}'", raw);
            return null;
        }
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse double: '{}'", raw);
            return null;
        }
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse integer: '{}'", raw);
            return null;
        }
    }
}

