package com.clinicare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.clinicare.model.Clinic;


@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    @Query("""
        SELECT DISTINCT c FROM Clinic c
        LEFT JOIN FETCH c.services
        WHERE c.active = true
        """)
    List<Clinic> findByActiveTrue();

    @Query("""
        SELECT DISTINCT c FROM Clinic c
        LEFT JOIN FETCH c.services s
        WHERE c.active = true
        AND (LOWER(s.serviceName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(s.category) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    List<Clinic> searchByServiceOrName(@Param("keyword") String keyword);
}