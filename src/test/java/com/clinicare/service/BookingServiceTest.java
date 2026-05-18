package com.clinicare.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository       bookingRepo;
    @Mock private ClinicRepository        clinicRepo;
    @Mock private ClinicServiceRepository serviceRepo;
    @Mock private TimeSlotRepository      slotRepo;
    @Mock private UserRepository          userRepo;
    @Mock private EmailService            emailService;
    @Mock private SimpMessagingTemplate   messaging;

    @InjectMocks
    private BookingService bookingService;

    private Clinic        clinic;
    private ClinicService service;
    private TimeSlot      slot;
    private User          user;

    @BeforeEach
    void setUp() {
        clinic = Clinic.builder()
                .id(1L).name("Test Clinic").build();

        service = ClinicService.builder()
                .id(1L).serviceName("General Checkup").clinic(clinic).build();

        slot = TimeSlot.builder()
                .id(1L).clinic(clinic)
                .date(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .isBooked(false).build();

        user = User.builder()
                .id(1L).name("John Doe")
                .email("john@example.com")
                .role(User.Role.USER).build();
    }

    @Test
    void guestBooking_success() {
        when(clinicRepo.findById(1L)).thenReturn(Optional.of(clinic));
        when(serviceRepo.findById(1L)).thenReturn(Optional.of(service));
        when(slotRepo.findById(1L)).thenReturn(Optional.of(slot));
        when(bookingRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createGuestBooking(
                1L, 1L, 1L,
                "Jane Guest", "jane@example.com", "090-0000-0000", "no notes"
        );

        assertThat(result.getGuestName()).isEqualTo("Jane Guest");
        assertThat(result.getGuestEmail()).isEqualTo("jane@example.com");
        assertThat(result.getStatus()).isEqualTo(Booking.Status.BOOKED);
        assertThat(result.getBookingRef()).startsWith("CLN-");
        assertThat(slot.getIsBooked()).isTrue();

        verify(slotRepo).save(slot);
        verify(bookingRepo).save(any(Booking.class));
    }

    @Test
    void guestBooking_slotAlreadyBooked_throwsException() {
        slot.setIsBooked(true);

        when(clinicRepo.findById(1L)).thenReturn(Optional.of(clinic));
        when(serviceRepo.findById(1L)).thenReturn(Optional.of(service));
        when(slotRepo.findById(1L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() ->
                bookingService.createGuestBooking(
                        1L, 1L, 1L,
                        "Jane Guest", "jane@example.com", "090-0000-0000", ""
                )
        ).isInstanceOf(RuntimeException.class)
         .hasMessage("Slot is already booked");

        verify(bookingRepo, never()).save(any());
    }

    @Test
    void guestBooking_clinicNotFound_throwsException() {
        when(clinicRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                bookingService.createGuestBooking(
                        99L, 1L, 1L,
                        "Jane", "jane@example.com", "090-0000-0000", ""
                )
        ).isInstanceOf(RuntimeException.class)
         .hasMessage("Clinic not found");
    }

    @Test
    void guestBooking_serviceNotFound_throwsException() {
        when(clinicRepo.findById(1L)).thenReturn(Optional.of(clinic));
        when(serviceRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                bookingService.createGuestBooking(
                        1L, 99L, 1L,
                        "Jane", "jane@example.com", "090-0000-0000", ""
                )
        ).isInstanceOf(RuntimeException.class)
         .hasMessage("Service not found");
    }

    @Test
    void userBooking_success() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(clinicRepo.findById(1L)).thenReturn(Optional.of(clinic));
        when(serviceRepo.findById(1L)).thenReturn(Optional.of(service));
        when(slotRepo.findById(1L)).thenReturn(Optional.of(slot));
        when(bookingRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createUserBooking(1L, 1L, 1L, 1L, "notes");

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStatus()).isEqualTo(Booking.Status.BOOKED);
        assertThat(result.getBookingRef()).startsWith("CLN-");
        assertThat(slot.getIsBooked()).isTrue();
    }

    @Test
    void userBooking_slotAlreadyBooked_throwsException() {
        slot.setIsBooked(true);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(clinicRepo.findById(1L)).thenReturn(Optional.of(clinic));
        when(serviceRepo.findById(1L)).thenReturn(Optional.of(service));
        when(slotRepo.findById(1L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() ->
                bookingService.createUserBooking(1L, 1L, 1L, 1L, "")
        ).isInstanceOf(RuntimeException.class)
         .hasMessage("Slot is already booked");
    }

    @Test
    void bookingRef_startsWithCLN() {
        when(clinicRepo.findById(1L)).thenReturn(Optional.of(clinic));
        when(serviceRepo.findById(1L)).thenReturn(Optional.of(service));
        when(slotRepo.findById(1L)).thenReturn(Optional.of(slot));
        when(bookingRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Booking result = bookingService.createGuestBooking(
                1L, 1L, 1L, "Jane", "jane@example.com", "090-0000-0000", ""
        );

        assertThat(result.getBookingRef()).startsWith("CLN-");
        assertThat(result.getBookingRef()).hasSize(12); // CLN-(4) + 8 chars
    }

    @Test
    void twoBookings_haveDifferentRefs() {
        when(clinicRepo.findById(1L)).thenReturn(Optional.of(clinic));
        when(serviceRepo.findById(1L)).thenReturn(Optional.of(service));
        when(bookingRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TimeSlot slot1 = TimeSlot.builder().id(1L).clinic(clinic)
                .date(LocalDate.now()).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30)).isBooked(false).build();

        TimeSlot slot2 = TimeSlot.builder().id(2L).clinic(clinic)
                .date(LocalDate.now()).startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30)).isBooked(false).build();

        when(slotRepo.findById(1L)).thenReturn(Optional.of(slot1));
        Booking b1 = bookingService.createGuestBooking(
                1L, 1L, 1L, "Jane", "jane@example.com", "090-0000-0000", "");

        when(slotRepo.findById(2L)).thenReturn(Optional.of(slot2));
        Booking b2 = bookingService.createGuestBooking(
                1L, 1L, 2L, "John", "john@example.com", "090-1111-1111", "");

        assertThat(b1.getBookingRef()).isNotEqualTo(b2.getBookingRef());
    }
}