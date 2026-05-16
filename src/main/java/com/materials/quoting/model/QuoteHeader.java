package com.materials.quoting.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quote_header")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Long quoteId;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_status", length = 20)
    private QuoteStatus quoteStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battery_id", referencedColumnName = "id", nullable = false)
    private CarBatteryBreakdown battery;

    @Column(name = "battery_qty")
    private Integer batteryQty;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Builder.Default
    @JsonManagedReference
    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteDetail> details = new ArrayList<>();
}
