package com.clinicare.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicare.model.Booking;
import com.clinicare.repository.BookingRepository;
import com.clinicare.repository.TimeSlotRepository;
import com.clinicare.repository.UserRepository;
import com.clinicare.service.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService     bookingService;
    private final BookingRepository  bookingRepo;
    private final UserRepository     userRepo;
    private final TimeSlotRepository slotRepo;

    @PostMapping("/guest")
    public ResponseEntity<?> guestBook(@Valid @RequestBody GuestBookingRequest req) {
        try {
            Booking booking = bookingService.createGuestBooking(
                    req.clinicId(), req.serviceId(), req.timeSlotId(),
                    req.guestName(), req.guestEmail(), req.guestPhone(), req.notes()
            );
            return ResponseEntity.ok(Map.of(
                    "bookingRef", booking.getBookingRef(),
                    "status",     booking.getStatus(),
                    "message",    "Booking confirmed! Ref: " + booking.getBookingRef()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> userBook(@Valid @RequestBody UserBookingRequest req, Authentication auth) {
        try {
            var user = userRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Booking booking = bookingService.createUserBooking(
                    user.getId(), req.clinicId(), req.serviceId(), req.timeSlotId(), req.notes()
            );
            return ResponseEntity.ok(Map.of(
                    "bookingRef", booking.getBookingRef(),
                    "status",     booking.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> myBookings(Authentication auth) {
        var user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Use fetch join query to load everything in 1 query
        List<Booking> bookings = bookingRepo.findByUserIdWithDetails(user.getId());

        return ResponseEntity.ok(bookings.stream().map(b -> Map.of(
                "id",          b.getId(),
                "bookingRef",  b.getBookingRef(),
                "clinicName",  b.getClinic().getName(),
                "serviceName", b.getService().getServiceName(),
                "price",       b.getService().getPrice(),
                "date",        b.getTimeSlot().getDate().toString(),
                "startTime",   b.getTimeSlot().getStartTime().toString(),
                "status",      b.getStatus()
        )).toList());
    }

    @GetMapping("/ref/{ref}")
    public ResponseEntity<?> getByRef(@PathVariable String ref) {
        return bookingRepo.findByBookingRefWithDetails(ref)
                .map(b -> ResponseEntity.ok(Map.of(
                        "bookingRef",  b.getBookingRef(),
                        "clinicName",  b.getClinic().getName(),
                        "serviceName", b.getService().getServiceName(),
                        "date",        b.getTimeSlot().getDate().toString(),
                        "startTime",   b.getTimeSlot().getStartTime().toString(),
                        "status",      b.getStatus()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id, Authentication auth) {
        Booking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getUser() == null ||
            !booking.getUser().getEmail().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not your booking"));
        }

        if (booking.getStatus() == Booking.Status.COMPLETED ||
            booking.getStatus() == Booking.Status.CANCELLED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot cancel this booking"));
        }

        var slot = booking.getTimeSlot();
        slot.setIsBooked(false);
        slotRepo.save(slot);

        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepo.save(booking);
        return ResponseEntity.ok(Map.of("cancelled", true));
    }

    record GuestBookingRequest(
            @NotNull Long clinicId, @NotNull Long serviceId, @NotNull Long timeSlotId,
            @NotBlank String guestName, @Email @NotBlank String guestEmail,
            @NotBlank String guestPhone, String notes
    ) {}

    record UserBookingRequest(
            @NotNull Long clinicId, @NotNull Long serviceId,
            @NotNull Long timeSlotId, String notes
    ) {}
}