package com.clinicare.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clinics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String address;

    private String phone;

    private String imageUrl;

    private String description;

    // Geographic coordinates for distance calc
    private Double latitude;
    private Double longitude;

    // Computed from reviews — updated when review added
    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ClinicService> services;

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TimeSlot> timeSlots;

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}
