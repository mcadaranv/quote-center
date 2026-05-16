package com.materials.quoting.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quote_rejection_reasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteRejectionReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reason_id")
    private Long reasonId;

    @Column(name = "reason_name", length = 255)
    private String reasonName;

    @Builder.Default
    @Column(name = "active_fl", nullable = false)
    private Boolean activeFl = true;
}

