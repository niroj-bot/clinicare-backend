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
    List<Booking> findByUserId(Long userId);
    List<Booking> findByClinicId(Long clinicId);
    Optional<Booking> findByBookingRef(String bookingRef);
    List<Booking> findByGuestEmail(String guestEmail);
    @Query("SELECT b FROM Booking b JOIN FETCH b.clinic JOIN FETCH b.service JOIN FETCH b.timeSlot WHERE b.user.id = :userId ORDER BY b.id DESC")
    List<Booking> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.clinic JOIN FETCH b.service JOIN FETCH b.timeSlot WHERE b.bookingRef = :ref")
    Optional<Booking> findByBookingRefWithDetails(@Param("ref") String ref);
}
