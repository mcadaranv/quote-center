package com.materials.quoting.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quote_rejections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", referencedColumnName = "quote_id", nullable = false)
    private QuoteHeader quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_id", referencedColumnName = "reason_id")
    private QuoteRejectionReason reason;

    @Column(name = "other_value", length = 500)
    private String otherValue;

    @Column(name = "comments", length = 2000)
    private String comments;
}

