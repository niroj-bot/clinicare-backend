package com.clinicare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.clinicare.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Fetch all 
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.clinic " +
           "JOIN FETCH b.service " +
           "JOIN FETCH b.timeSlot " +
           "LEFT JOIN FETCH b.user " +
           "WHERE b.clinic.id = :clinicId " +
           "ORDER BY b.timeSlot.date DESC, b.timeSlot.startTime ASC")
    List<Booking> findByClinicIdWithDetails(@Param("clinicId") Long clinicId);

    // For admin
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.clinic " +
           "JOIN FETCH b.service " +
           "JOIN FETCH b.timeSlot " +
           "LEFT JOIN FETCH b.user " +
           "ORDER BY b.timeSlot.date DESC")
    List<Booking> findAllWithDetails();

    // For user bookings
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.clinic " +
           "JOIN FETCH b.service " +
           "JOIN FETCH b.timeSlot " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.timeSlot.date DESC")
    List<Booking> findByUserIdWithDetails(@Param("userId") Long userId);

    List<Booking> findByUserId(Long userId);
    List<Booking> findByClinicId(Long clinicId);
    Optional<Booking> findByBookingRef(String bookingRef);
    List<Booking> findByGuestEmail(String guestEmail);
}