package com.materials.quoting.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "car_battery_breakdown")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarBatteryBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "make")
    private String make;

    @Column(name = "model")
    private String model;

    @Column(name = "trim")
    private String trim;

    @Column(name = "propulsion_type")
    private String propulsionType;

    /** Numeric portion of the battery capacity in kWh (e.g. 21.3 for "21.3 kWh"). */
    @Column(name = "battery_capacity_kwh")
    private Double batteryCapacityKwh;

    @Column(name = "model_year_start")
    private Integer modelYearStart;

    @Column(name = "model_year_end")
    private Integer modelYearEnd;

    @Column(name = "pack_weight_kg")
    private Double packWeightKg;

    @Column(name = "ni_weight_kg")
    private Double niWeightKg;

    @Column(name = "co_weight_kg")
    private Double coWeightKg;

    @Column(name = "li_weight_kg")
    private Double liWeightKg;

    @Column(name = "cu_weight_kg")
    private Double cuWeightKg;

    @Column(name = "al_weight_kg")
    private Double alWeightKg;
}

