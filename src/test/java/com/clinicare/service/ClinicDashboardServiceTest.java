package com.clinicare.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clinicare.exception.BadRequestException;
import com.clinicare.exception.ResourceNotFoundException;
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
class ClinicDashboardServiceTest {

    @Mock private UserRepository          userRepo;
    @Mock private ClinicRepository        clinicRepo;
    @Mock private BookingRepository       bookingRepo;
    @Mock private TimeSlotRepository      slotRepo;
    @Mock private ClinicServiceRepository serviceRepo;

    @InjectMocks
    private ClinicDashboardService dashboardService;

    private Clinic clinic;
    private User   clinicUser;
    private User   userNoClinic;

    @BeforeEach
    void setUp() {
        clinic = Clinic.builder()
                .id(1L).name("Test Clinic")
                .address("Tokyo, Japan").phone("03-1234-5678")
                .averageRating(4.5).reviewCount(10).build();

        clinicUser = User.builder()
                .id(1L).name("Clinic Admin")
                .email("clinic@example.com")
                .role(User.Role.CLINIC)
                .managedClinic(clinic).build();

        userNoClinic = User.builder()
                .id(2L).name("No Clinic User")
                .email("noclinic@example.com")
                .role(User.Role.CLINIC)
                .managedClinic(null).build();
    }

    @Test
    void getClinicForUser_success() {
        when(userRepo.findByEmailWithClinic("clinic@example.com"))
                .thenReturn(Optional.of(clinicUser));

        Clinic result = dashboardService.getClinicForUser("clinic@example.com");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Clinic");
    }

    @Test
    void getClinicForUser_userNotFound_throwsException() {
        when(userRepo.findByEmailWithClinic("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                dashboardService.getClinicForUser("unknown@example.com")
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getClinicForUser_noClinicAssigned_throwsException() {
        when(userRepo.findByEmailWithClinic("noclinic@example.com"))
                .thenReturn(Optional.of(userNoClinic));

        assertThatThrownBy(() ->
                dashboardService.getClinicForUser("noclinic@example.com")
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getDashboard_returnsCorrectStats() {
        when(userRepo.findByEmailWithClinic("clinic@example.com"))
                .thenReturn(Optional.of(clinicUser));

        TimeSlot todaySlot = TimeSlot.builder()
                .date(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .isBooked(true).build();

        TimeSlot oldSlot = TimeSlot.builder()
                .date(LocalDate.now().minusDays(5))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .isBooked(true).build();

        ClinicService svc = ClinicService.builder()
                .id(1L).serviceName("Checkup").clinic(clinic).build();

        Booking b1 = Booking.builder()
                .id(1L).bookingRef("CLN-AAA").clinic(clinic).service(svc)
                .timeSlot(todaySlot).guestName("Guest A").guestEmail("a@test.com")
                .status(Booking.Status.BOOKED).build();

        Booking b2 = Booking.builder()
                .id(2L).bookingRef("CLN-BBB").clinic(clinic).service(svc)
                .timeSlot(todaySlot).guestName("Guest B").guestEmail("b@test.com")
                .status(Booking.Status.CONFIRMED).build();

        Booking b3 = Booking.builder()
                .id(3L).bookingRef("CLN-CCC").clinic(clinic).service(svc)
                .timeSlot(oldSlot).guestName("Guest C").guestEmail("c@test.com")
                .status(Booking.Status.COMPLETED).build();

        when(bookingRepo.findByClinicIdWithDetails(1L))
                .thenReturn(List.of(b1, b2, b3));

        Map<String, Object> result = dashboardService.getDashboard("clinic@example.com");

        assertThat(result.get("clinicName")).isEqualTo("Test Clinic");
        assertThat(result.get("todayTotal")).isEqualTo(2);
        assertThat(result.get("todayBooked")).isEqualTo(1L);
        assertThat(result.get("todayConfirmed")).isEqualTo(1L);
        assertThat(result.get("totalBookings")).isEqualTo(3);
    }

    @Test
    void getDashboard_noBookings_returnsZeros() {
        when(userRepo.findByEmailWithClinic("clinic@example.com"))
                .thenReturn(Optional.of(clinicUser));
        when(bookingRepo.findByClinicIdWithDetails(1L))
                .thenReturn(List.of());

        Map<String, Object> result = dashboardService.getDashboard("clinic@example.com");

        assertThat(result.get("todayTotal")).isEqualTo(0);
        assertThat(result.get("totalBookings")).isEqualTo(0);
    }

    @Test
    void getBookings_filterByDate_returnsOnlyMatchingDate() {
        when(userRepo.findByEmailWithClinic("clinic@example.com"))
                .thenReturn(Optional.of(clinicUser));

        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        TimeSlot todaySlot = TimeSlot.builder().date(today)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30))
                .isBooked(true).build();

        TimeSlot tomorrowSlot = TimeSlot.builder().date(tomorrow)
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                .isBooked(true).build();

        ClinicService svc = ClinicService.builder()
                .id(1L).serviceName("Checkup").clinic(clinic).build();

        Booking b1 = Booking.builder().id(1L).bookingRef("CLN-AAA")
                .clinic(clinic).service(svc).timeSlot(todaySlot)
                .guestName("A").guestEmail("a@test.com")
                .status(Booking.Status.BOOKED).build();

        Booking b2 = Booking.builder().id(2L).bookingRef("CLN-BBB")
                .clinic(clinic).service(svc).timeSlot(tomorrowSlot)
                .guestName("B").guestEmail("b@test.com")
                .status(Booking.Status.BOOKED).build();

        when(bookingRepo.findByClinicIdWithDetails(1L))
                .thenReturn(List.of(b1, b2));

        List<Map<String, Object>> result = dashboardService.getBookings(
                "clinic@example.com", today.toString(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("bookingRef")).isEqualTo("CLN-AAA");
    }

    @Test
    void getBookings_filterByStatus_returnsOnlyMatchingStatus() {
        when(userRepo.findByEmailWithClinic("clinic@example.com"))
                .thenReturn(Optional.of(clinicUser));

        TimeSlot slot = TimeSlot.builder().date(LocalDate.now())
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30))
                .isBooked(true).build();

        ClinicService svc = ClinicService.builder()
                .id(1L).serviceName("Checkup").clinic(clinic).build();

        Booking booked = Booking.builder().id(1L).bookingRef("CLN-AAA")
                .clinic(clinic).service(svc).timeSlot(slot)
                .guestName("A").guestEmail("a@test.com")
                .status(Booking.Status.BOOKED).build();

        Booking cancelled = Booking.builder().id(2L).bookingRef("CLN-BBB")
                .clinic(clinic).service(svc).timeSlot(slot)
                .guestName("B").guestEmail("b@test.com")
                .status(Booking.Status.CANCELLED).build();

        when(bookingRepo.findByClinicIdWithDetails(1L))
                .thenReturn(List.of(booked, cancelled));

        List<Map<String, Object>> result = dashboardService.getBookings(
                "clinic@example.com", null, "BOOKED");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("bookingRef")).isEqualTo("CLN-AAA");
    }
}