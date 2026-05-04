package com.clinicare.service;

import com.clinicare.model.Clinic;
import com.clinicare.model.TimeSlot;
import com.clinicare.repository.ClinicRepository;
import com.clinicare.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotScheduler {

    private final ClinicRepository   clinicRepo;
    private final TimeSlotRepository slotRepo;

    // Standard working hours — 13 slots per day per clinic
    private static final LocalTime[] SLOT_TIMES = {
        // Morning: 09:00 - 12:00
        LocalTime.of(9,  0), LocalTime.of(9,  30),
        LocalTime.of(10, 0), LocalTime.of(10, 30),
        LocalTime.of(11, 0), LocalTime.of(11, 30),
        // Afternoon: 13:00 - 17:00
        LocalTime.of(13, 0), LocalTime.of(13, 30),
        LocalTime.of(14, 0), LocalTime.of(14, 30),
        LocalTime.of(15, 0), LocalTime.of(15, 30),
        LocalTime.of(16, 0), LocalTime.of(16, 30),
        // Evening: 18:00 - 20:00
        LocalTime.of(18, 0), LocalTime.of(18, 30),
        LocalTime.of(19, 0), LocalTime.of(19, 30),
    };

    /**
     * Runs every day at midnight — generates slots for next 7 days
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailySlots() {
        log.info("⏰ SlotScheduler — generating slots for next 7 days...");
        generate(7);
    }

    /**
     * Runs 30 seconds after app starts — ensures slots exist immediately
     */
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)
    public void generateOnStartup() {
        log.info("⏰ SlotScheduler startup — ensuring 7 days of slots exist...");
        generate(7);
    }

    /**
     * Cleanup job — runs every day at 2:00 AM
     * Deletes UNBOOKED slots older than 30 days
     * Keeps booked slots forever (needed for booking history)
     *
     * DB size stays fixed:
     *   Max rows = 50 clinics × 13 slots × 7 days = ~4,550 rows always
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanOldSlots() {
        LocalDate cutoff = LocalDate.now().minusDays(30);
        int deleted = slotRepo.deleteUnbookedSlotsBefore(cutoff);
        if (deleted > 0) {
            log.info("🧹 SlotScheduler cleanup — deleted {} old unbooked slots before {}",
                    deleted, cutoff);
        } else {
            log.info("🧹 SlotScheduler cleanup — nothing to clean");
        }
    }

    private void generate(int daysAhead) {
        List<Clinic> clinics = clinicRepo.findByActiveTrue();
        LocalDate today = LocalDate.now();
        int created = 0;

        for (Clinic clinic : clinics) {
            for (int day = 0; day < daysAhead; day++) {
                LocalDate date = today.plusDays(day);

                // Skip if slots already exist for this clinic + date
                if (slotRepo.countByClinicIdAndDate(clinic.getId(), date) > 0) continue;

                // Create all slots for this date
                for (LocalTime start : SLOT_TIMES) {
                    slotRepo.save(TimeSlot.builder()
                            .clinic(clinic).date(date)
                            .startTime(start).endTime(start.plusMinutes(30))
                            .waitMinutes(0).isBooked(false).build());
                    created++;
                }
            }
        }

        if (created > 0) {
            log.info("✅ SlotScheduler created {} new slots for {} clinics", created, clinics.size());
        } else {
            log.info("✅ SlotScheduler — all slots up to date");
        }
    }
}
