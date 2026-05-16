package com.materials.quoting.client.metals;

/**
 * Response from the metals pricing API's GET /price endpoint.
 *
 * <pre>
 * {
 *   "metal":    "nickel",
 *   "price":    18.45,
 *   "currency": "USD",
 *   "unit":     "kg"
 * }
 * </pre>
 */
public record MetalPriceResponse(
        String metal,
        double price,
        String currency,
        String unit
) {}

