package com.clinicare.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinicare.email.EmailService;
import com.clinicare.model.Booking;
import com.clinicare.model.Clinic;
import com.clinicare.model.ClinicService;
import com.clinicare.model.TimeSlot;
import com.clinicare.model.User;
import com.clinicare.repository.BookingRepository;
import com.clinicare.repository.ClinicRepository;
import com.clinicare.repository.ClinicServiceRepository;
import com.clinicare.repository.TimeSlotRepository;
import com.clinicare.repository.UserRepository;
import com.clinicare.websocket.SlotUpdateMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final ClinicRepository clinicRepo;
    private final ClinicServiceRepository serviceRepo;
    private final TimeSlotRepository slotRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    // only injected if WebSocket starter is active
    @Autowired(required = false)
    private SimpMessagingTemplate messaging;

    private void broadcastSlotUpdate(Long clinicId, Long slotId) {
        if (messaging != null) {
            try {
                messaging.convertAndSend(
                    "/topic/slots/" + clinicId,
                    new SlotUpdateMessage(clinicId, slotId, true)
                );
            } catch (Exception e) {
                
            }
        }
    }

    @Transactional
    public Booking createGuestBooking(Long clinicId, Long serviceId, Long timeSlotId,
            String guestName, String guestEmail, String guestPhone, String notes) {

        Clinic clinic     = clinicRepo.findById(clinicId).orElseThrow(() -> new RuntimeException("Clinic not found"));
        ClinicService svc = serviceRepo.findById(serviceId).orElseThrow(() -> new RuntimeException("Service not found"));
        TimeSlot slot     = slotRepo.findById(timeSlotId).orElseThrow(() -> new RuntimeException("Slot not found"));
        if (slot.getIsBooked()) throw new RuntimeException("Slot is already booked");

        slot.setIsBooked(true);
        slotRepo.save(slot);
        broadcastSlotUpdate(clinicId, slot.getId());

        Booking booking = Booking.builder()
                .clinic(clinic).service(svc).timeSlot(slot)
                .guestName(guestName).guestEmail(guestEmail).guestPhone(guestPhone)
                .notes(notes).bookingRef(generateRef()).status(Booking.Status.BOOKED)
                .build();
        Booking saved = bookingRepo.save(booking);

        emailService.sendBookingConfirmation(
            guestEmail, guestName, saved.getBookingRef(),
            clinic.getName(), svc.getServiceName(),
            slot.getDate().toString(), slot.getStartTime().toString()
        );
        return saved;
    }

    @Transactional
    public Booking createUserBooking(Long userId, Long clinicId, Long serviceId, Long timeSlotId, String notes) {
        User user         = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Clinic clinic     = clinicRepo.findById(clinicId).orElseThrow(() -> new RuntimeException("Clinic not found"));
        ClinicService svc = serviceRepo.findById(serviceId).orElseThrow(() -> new RuntimeException("Service not found"));
        TimeSlot slot     = slotRepo.findById(timeSlotId).orElseThrow(() -> new RuntimeException("Slot not found"));
        if (slot.getIsBooked()) throw new RuntimeException("Slot is already booked");

        slot.setIsBooked(true);
        slotRepo.save(slot);
        broadcastSlotUpdate(clinicId, slot.getId());

        Booking booking = Booking.builder()
                .user(user).clinic(clinic).service(svc).timeSlot(slot)
                .notes(notes).bookingRef(generateRef()).status(Booking.Status.BOOKED)
                .build();
        Booking saved = bookingRepo.save(booking);

        emailService.sendBookingConfirmation(
            user.getEmail(), user.getName(), saved.getBookingRef(),
            clinic.getName(), svc.getServiceName(),
            slot.getDate().toString(), slot.getStartTime().toString()
        );
        return saved;
    }

    private String generateRef() {
        return "CLN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
