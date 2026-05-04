package com.clinicare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.clinicare.model.ClinicService;

@Repository
public interface ClinicServiceRepository extends JpaRepository<ClinicService, Long> {
    List<ClinicService> findByClinicId(Long clinicId);
    List<ClinicService> findByClinicIdAndAvailableTrue(Long clinicId);
    List<ClinicService> findByClinicIdIn(List<Long> clinicIds);
    List<ClinicService> findByClinicIdInAndAvailableTrue(List<Long> clinicIds);
}
