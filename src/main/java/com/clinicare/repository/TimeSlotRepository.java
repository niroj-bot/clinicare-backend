package com.clinicare.repository;

import com.clinicare.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    // Single clinic, single date, available only
    List<TimeSlot> findByClinicIdAndDateAndIsBookedFalseOrderByStartTime(Long clinicId, LocalDate date);

    // Single clinic, all future available slots
    List<TimeSlot> findByClinicIdAndIsBookedFalseOrderByDateAscStartTimeAsc(Long clinicId);

    // Single clinic, date range, available only — used by ClinicController
    List<TimeSlot> findByClinicIdAndDateBetweenAndIsBookedFalseOrderByDateAscStartTimeAsc(
            Long clinicId, LocalDate from, LocalDate to);

    // Multiple clinics batch fetch — used by ClinicSearchService + AdminController
    List<TimeSlot> findByClinicIdInAndIsBookedFalseOrderByDateAscStartTimeAsc(List<Long> clinicIds);

    // All slots for a date (booked + available) — for availability view
    List<TimeSlot> findByClinicIdAndDateOrderByStartTime(Long clinicId, LocalDate date);

    // Count slots — used by scheduler to skip already generated dates
    long countByClinicIdAndDate(Long clinicId, LocalDate date);

    // Prevent duplicate slot creation
    long countByClinicIdAndDateAndStartTime(Long clinicId, LocalDate date, LocalTime startTime);

    // Cleanup — delete old UNBOOKED slots (keeps booked ones for booking history)
    @Modifying
    @Query("DELETE FROM TimeSlot t WHERE t.date < :cutoff AND t.isBooked = false")
    int deleteUnbookedSlotsBefore(@Param("cutoff") LocalDate cutoff);
}
