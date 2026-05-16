package com.materials.quoting.client.metals;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Metals supported by the metals pricing API.
 * Values are lowercase to match the Python API's enum exactly.
 */
public enum Metal {

    NICKEL("nickel"),
    COBALT("cobalt"),
    LITHIUM("lithium"),
    COPPER("copper");

    private final String value;

    Metal(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Look up a Metal by its API string value (case-insensitive).
     *
     * @throws IllegalArgumentException if the value does not match any Metal
     */
    public static Metal fromValue(String value) {
        return tryFromValue(value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown metal: '" + value + "'. Supported values: nickel, cobalt, lithium, copper"));
    }

    /**
     * Safe lookup — returns {@link java.util.Optional#empty()} for unrecognised values
     * instead of throwing.
     */
    public static java.util.Optional<Metal> tryFromValue(String value) {
        if (value == null || value.isBlank()) return java.util.Optional.empty();
        for (Metal m : values()) {
            if (m.value.equalsIgnoreCase(value.trim())) return java.util.Optional.of(m);
        }
        return java.util.Optional.empty();
    }

    @Override
    public String toString() {
        return value;
    }
}

