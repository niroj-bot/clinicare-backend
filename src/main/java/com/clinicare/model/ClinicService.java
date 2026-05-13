package com.clinicare.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String category;  

    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // Duration in minutes (used for Time-to-Treatment calc)
    @Builder.Default
    private Integer durationMinutes = 30;

    @Builder.Default
    private Boolean available = true;
}
