package com.materials.quoting.model;

/**
 * Search criteria for querying {@code car_battery_breakdown}.
 * {@code make} and {@code model} are always required.
 * {@code trim} and {@code year} are optional — when supplied they narrow the result.
 */
public record CarSearch(
        String make,
        String model,
        String trim,
        Integer year
) {
    public CarSearch {
        if (make == null || make.isBlank())  throw new IllegalArgumentException("make is required");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
    }
}

