package com.clinicare.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "time_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    @Builder.Default
    private Boolean isBooked = false;

    // Estimated wait time in minutes before this slot
    @Builder.Default
    private Integer waitMinutes = 0;
}
