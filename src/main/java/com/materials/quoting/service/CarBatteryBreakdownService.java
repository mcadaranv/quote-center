package com.materials.quoting.service;

import com.materials.quoting.model.CarBatteryBreakdown;
import com.materials.quoting.model.CarSearch;

import java.util.List;
import java.util.Optional;

public interface CarBatteryBreakdownService {

    /** Create a new record. */
    CarBatteryBreakdown create(CarBatteryBreakdown entry);

    /** Retrieve a single record by its ID. */
    Optional<CarBatteryBreakdown> findById(Long id);

    /** Retrieve all records. */
    List<CarBatteryBreakdown> findAll();

    /**
     * Search by the criteria in {@link CarSearch}.
     * make and model are always required; trim and year are optional filters.
     */
    List<CarBatteryBreakdown> findBySearch(CarSearch search);

    /** Update an existing record. Throws if not found. */
    CarBatteryBreakdown update(Long id, CarBatteryBreakdown updated);

    /** Delete a record by its ID. */
    void delete(Long id);
}
