package com.clinicare.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insurance_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Japanese insurance type
    private String insuranceType;   // Employee, National, MutualAid, Elderly

    private String insuranceName;   // Display name in Japanese/English
    private String policyNumber;
    private String holderName;
}
