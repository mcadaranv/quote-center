package com.materials.quoting.client.metals;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * HTTP client for the metals pricing Python API.
 *
 * <p>Endpoint: {@code GET /price?metal={nickel|cobalt|lithium|copper}}
 *
 * <p>The remote API randomly returns a 503 ~25% of the time.
 * {@link #getPrice(Metal)} will retry up to {@code maxRetries} times before
 * throwing a {@link MetalsPricingException}.
 */
@Slf4j
@Component
public class MetalsPricingClient {

    private static final int MAX_RETRIES = 3;

    private final RestClient restClient;

    public MetalsPricingClient(@Qualifier("metalsPricingRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetch the current spot price for a single metal.
     *
     * @param metal the metal to price
     * @return the price response from the API
     * @throws MetalsPricingException if the service is unavailable after retries,
     *                                or any other non-2xx response is received
     */
    public MetalPriceResponse getPrice(Metal metal) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                MetalPriceResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/price")
                                .queryParam("metal", metal.getValue())
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                            throw new MetalsPricingException(
                                    "Metals pricing service returned " + res.getStatusCode() +
                                    " for metal: " + metal.getValue());
                        })
                        .body(MetalPriceResponse.class);

                log.debug("Fetched price for {}: {} {}/{}", metal, response.price(),
                        response.currency(), response.unit());
                return response;

            } catch (MetalsPricingException ex) {
                if (attempt == MAX_RETRIES) {
                    log.error("Metals pricing service unavailable after {} attempts for {}", MAX_RETRIES, metal);
                    throw ex;
                }
                log.warn("Attempt {}/{} failed for {} – retrying. Reason: {}",
                        attempt, MAX_RETRIES, metal, ex.getMessage());
            } catch (RestClientResponseException ex) {
                throw new MetalsPricingException(
                        "Unexpected error fetching price for " + metal.getValue() + ": " + ex.getMessage(), ex);
            }
        }
        // unreachable – satisfies compiler
        throw new MetalsPricingException("Failed to fetch price for " + metal.getValue());
    }

    /**
     * Convenience method – fetch prices for all supported metals in one call per metal.
     *
     * @return map of Metal → MetalPriceResponse
     */
    public Map<Metal, MetalPriceResponse> getAllPrices() {
        Map<Metal, MetalPriceResponse> prices = new java.util.LinkedHashMap<>();
        for (Metal metal : Metal.values()) {
            prices.put(metal, getPrice(metal));
        }
        return prices;
    }
}

