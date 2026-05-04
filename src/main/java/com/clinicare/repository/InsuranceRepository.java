package com.clinicare.repository;

import com.clinicare.model.InsuranceCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceRepository extends JpaRepository<InsuranceCard, Long> {
    Optional<InsuranceCard> findByUserId(Long userId);
}
