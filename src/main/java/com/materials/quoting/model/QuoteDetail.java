package com.materials.quoting.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "quote_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", referencedColumnName = "quote_id", nullable = false)
    private QuoteHeader quote;

    @Column(name = "metal", length = 100)
    private String metal;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "price_per_amount", precision = 19, scale = 4)
    private BigDecimal pricePerAmount;

    @Column(name = "value_in_dollars", precision = 19, scale = 2)
    private BigDecimal valueInDollars;
}

