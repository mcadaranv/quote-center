package com.materials.quoting.repository;

import com.materials.quoting.model.CarBatteryBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarBatteryBreakdownRepository extends JpaRepository<CarBatteryBreakdown, Long> {

    /**
     * Flexible search by make + model (required) with optional trim and year.
     * <ul>
     *   <li>trim = null  → ignored</li>
     *   <li>year = null  → ignored; otherwise matched against [modelYearStart, modelYearEnd]
     *       where a null modelYearEnd means the model is still current.</li>
     * </ul>
     */
    @Query("SELECT c FROM CarBatteryBreakdown c " +
           "WHERE LOWER(c.make)  = LOWER(:make) " +
           "AND   LOWER(c.model) = LOWER(:model) " +
           "AND   (:trim IS NULL OR LOWER(c.trim) = LOWER(:trim)) " +
           "AND   (:year IS NULL OR " +
           "       (c.modelYearStart <= :year AND (c.modelYearEnd IS NULL OR c.modelYearEnd >= :year)))")
    List<CarBatteryBreakdown> findBySearch(
            @Param("make")  String make,
            @Param("model") String model,
            @Param("trim")  String trim,
            @Param("year")  Integer year);
}
