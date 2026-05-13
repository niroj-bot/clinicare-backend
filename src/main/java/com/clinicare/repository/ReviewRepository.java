package com.clinicare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.clinicare.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByClinicIdOrderByCreatedAtDesc(Long clinicId);

    // Find existing review for update/edit
    Optional<Review> findByUserIdAndClinicId(Long userId, Long clinicId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.clinic.id = :clinicId")
    Double findAverageRatingByClinicId(@Param("clinicId") Long clinicId);

    long countByClinicId(Long clinicId);
}
