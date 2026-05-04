package com.clinicare.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable — if null, it's a guest booking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ClinicService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    // Guest fields (filled when user is null)
    private String guestName;
    private String guestEmail;
    private String guestPhone;

    // Unique reference code shown to user after booking
    @Column(unique = true)
    private String bookingRef;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.BOOKED;

    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        BOOKED, CONFIRMED, COMPLETED, CANCELLED
    }
}
