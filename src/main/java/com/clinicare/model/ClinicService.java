package com.clinicare.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "clinic_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    private String serviceName;

    private String category;  // e.g. "Blood Test", "X-Ray", "Dental"

    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // Duration in minutes (used for Time-to-Treatment calc)
    @Builder.Default
    private Integer durationMinutes = 30;

    @Builder.Default
    private Boolean available = true;
}
