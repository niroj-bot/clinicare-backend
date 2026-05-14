package com.clinicare.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinicare.exception.BadRequestException;
import com.clinicare.exception.ResourceNotFoundException;
import com.clinicare.exception.UnauthorizedException;
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

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ClinicDashboardService {

    private final UserRepository          userRepo;
    private final ClinicRepository        clinicRepo;
    private final BookingRepository       bookingRepo;
    private final TimeSlotRepository      slotRepo;
    private final ClinicServiceRepository serviceRepo;

    public Clinic getClinicForUser(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getManagedClinic() == null)
            throw new BadRequestException("No clinic assigned to this account");
        return user.getManagedClinic();
    }

    public Map<String, Object> getDashboard(String email) {
        Clinic c = getClinicForUser(email);
        List<Booking> all   = bookingRepo.findByClinicId(c.getId());
        List<Booking> today = all.stream()
                .filter(b -> b.getTimeSlot().getDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clinicId",     c.getId());
        result.put("clinicName",   c.getName());
        result.put("todayTotal",   today.size());
        result.put("todayBooked",  today.stream().filter(b -> b.getStatus() == Booking.Status.BOOKED).count());
        result.put("todayConfirmed", today.stream().filter(b -> b.getStatus() == Booking.Status.CONFIRMED).count());
        result.put("totalBookings", all.size());
        result.put("totalPatients", all.stream()
                .map(b -> b.getUser() != null ? "u" + b.getUser().getId() : "g" + b.getGuestEmail())
                .distinct().count());
        result.put("todayAppointments", today.stream().map(this::bookingToMap).collect(Collectors.toList()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBookings(String email, String date, String status) {
        Clinic c = getClinicForUser(email);
        List<Booking> list = bookingRepo.findByClinicId(c.getId());
        if (date != null && !date.isBlank()) {
            LocalDate d = LocalDate.parse(date);
            list = list.stream().filter(b -> b.getTimeSlot().getDate().equals(d)).collect(Collectors.toList());
        }
        if (status != null && !status.equals("ALL")) {
            Booking.Status s = Booking.Status.valueOf(status);
            list = list.stream().filter(b -> b.getStatus() == s).collect(Collectors.toList());
        }
        return list.stream().map(this::bookingToMap).collect(Collectors.toList());
    }

    @Transactional
    public void updateBookingStatus(Long bookingId, String status) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        b.setStatus(Booking.Status.valueOf(status));
        bookingRepo.save(b);
    }

    public List<Map<String, Object>> getServices(String email) {
        Clinic c = getClinicForUser(email);
        return serviceRepo.findByClinicId(c.getId()).stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",              s.getId());
            m.put("serviceName",     s.getServiceName());
            m.put("category",        s.getCategory());
            m.put("price",           s.getPrice());
            m.put("durationMinutes", s.getDurationMinutes());
            m.put("available",       s.getAvailable());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addService(String email, String serviceName, String category,
                           String description, BigDecimal price, Integer durationMinutes) {
        Clinic c = getClinicForUser(email);
        serviceRepo.save(ClinicService.builder()
                .clinic(c).serviceName(serviceName).category(category)
                .description(description).price(price)
                .durationMinutes(durationMinutes != null ? durationMinutes : 30)
                .available(true).build());
    }

    @Transactional
    public void updateService(Long serviceId, String serviceName, String category,
                              BigDecimal price, Integer durationMinutes, Boolean available) {
        ClinicService svc = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (serviceName     != null) svc.setServiceName(serviceName);
        if (category        != null) svc.setCategory(category);
        if (price           != null) svc.setPrice(price);
        if (durationMinutes != null) svc.setDurationMinutes(durationMinutes);
        if (available       != null) svc.setAvailable(available);
        serviceRepo.save(svc);
    }

    public List<Map<String, Object>> getSlots(String email, String date) {
        Clinic c = getClinicForUser(email);
        List<TimeSlot> slots = date != null && !date.isBlank()
                ? slotRepo.findByClinicIdAndDateAndIsBookedFalseOrderByStartTime(c.getId(), LocalDate.parse(date))
                : slotRepo.findByClinicIdAndIsBookedFalseOrderByDateAscStartTimeAsc(c.getId());
        return slots.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        s.getId());
            m.put("date",      s.getDate().toString());
            m.put("startTime", s.getStartTime().toString());
            m.put("endTime",   s.getEndTime().toString());
            m.put("isBooked",  s.getIsBooked());
            return m;
        }).collect(Collectors.toList());
    }

    // Get ALL slots for a date 
    public Map<String, Object> getSlotAvailability(String email, String date) {
        Clinic c = getClinicForUser(email);
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<TimeSlot> all = slotRepo.findByClinicIdAndDateOrderByStartTime(c.getId(), d);

        List<Map<String, Object>> slotList = all.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        s.getId());
            m.put("startTime", s.getStartTime().toString());
            m.put("endTime",   s.getEndTime().toString());
            m.put("isBooked",  s.getIsBooked());
            return m;
        }).collect(Collectors.toList());

        long available = all.stream().filter(s -> !s.getIsBooked()).count();
        long booked    = all.stream().filter(TimeSlot::getIsBooked).count();

        return Map.of(
            "date",      d.toString(),
            "total",     all.size(),
            "available", available,
            "booked",    booked,
            "slots",     slotList
        );
    }

    @Transactional
    public void addSlot(String email, String date, String startTime, String endTime) {
        Clinic c = getClinicForUser(email);
        LocalDate d  = LocalDate.parse(date);
        LocalTime st = LocalTime.parse(startTime);
        LocalTime et = LocalTime.parse(endTime);

        // Prevent duplicate slots
        long existing = slotRepo.countByClinicIdAndDateAndStartTime(c.getId(), d, st);
        if (existing > 0) throw new BadRequestException("Slot already exists for this time");

        slotRepo.save(TimeSlot.builder()
                .clinic(c).date(d).startTime(st).endTime(et)
                .waitMinutes(0).isBooked(false).build());
    }

    public Map<String, Object> getClinicInfo(String email) {
        Clinic c = getClinicForUser(email);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            c.getId());
        m.put("name",          c.getName());
        m.put("address",       c.getAddress());
        m.put("phone",         c.getPhone());
        m.put("description",   c.getDescription());
        m.put("latitude",      c.getLatitude());
        m.put("longitude",     c.getLongitude());
        m.put("averageRating", c.getAverageRating());
        m.put("reviewCount",   c.getReviewCount());
        return m;
    }

    @Transactional
    public void updateClinicInfo(String email, String name, String address,
                                  String phone, String description) {
        Clinic c = getClinicForUser(email);
        if (name        != null) c.setName(name);
        if (address     != null) c.setAddress(address);
        if (phone       != null) c.setPhone(phone);
        if (description != null) c.setDescription(description);
        clinicRepo.save(c);
    }

    private Map<String, Object> bookingToMap(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         b.getId());
        m.put("bookingRef", b.getBookingRef());
        m.put("patient",    b.getUser() != null ? b.getUser().getName() : b.getGuestName());
        m.put("phone",      b.getUser() != null ? b.getUser().getPhone() : b.getGuestPhone());
        m.put("service",    b.getService().getServiceName());
        m.put("date",       b.getTimeSlot().getDate().toString());
        m.put("time",       b.getTimeSlot().getStartTime().toString());
        m.put("status",     b.getStatus().name());
        m.put("notes",      b.getNotes());
        return m;
    }

    @Transactional
    public void deleteSlot(String email, Long slotId) {
        Clinic c = getClinicForUser(email);
        TimeSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        if (!slot.getClinic().getId().equals(c.getId()))
            throw new UnauthorizedException("Not your clinic slot");
        if (slot.getIsBooked())
            throw new BadRequestException("Cannot delete a booked slot");
        slotRepo.delete(slot);
    }

}