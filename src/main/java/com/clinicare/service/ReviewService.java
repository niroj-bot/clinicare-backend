package com.clinicare.service;

import com.clinicare.model.Clinic;
import com.clinicare.model.Review;
import com.clinicare.model.User;
import com.clinicare.repository.ClinicRepository;
import com.clinicare.repository.ReviewRepository;
import com.clinicare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ClinicRepository  clinicRepo;
    private final UserRepository    userRepo;

    public List<Review> getClinicReviews(Long clinicId) {
        return reviewRepo.findByClinicIdOrderByCreatedAtDesc(clinicId);
    }

    @Transactional
    public Review addOrUpdateReview(Long clinicId, Long userId,
                                    String reviewerName, int rating, String comment) {
        Clinic clinic = clinicRepo.findById(clinicId)
                .orElseThrow(() -> new RuntimeException("Clinic not found"));

        Review review;

        if (userId != null) {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // If user already reviewed this clinic → UPDATE existing review
            review = reviewRepo.findByUserIdAndClinicId(userId, clinicId)
                    .orElse(Review.builder().clinic(clinic).user(user)
                            .reviewerName(user.getName()).build());

            review.setRating(rating);
            review.setComment(comment);
            review.setReviewerName(user.getName());
        } else {
            // Guest — always create new review
            review = Review.builder()
                    .clinic(clinic)
                    .rating(rating)
                    .comment(comment)
                    .reviewerName(reviewerName != null ? reviewerName : "Anonymous")
                    .build();
        }

        Review saved = reviewRepo.save(review);

        // Update clinic's cached average rating
        Double avg  = reviewRepo.findAverageRatingByClinicId(clinicId);
        long   count = reviewRepo.countByClinicId(clinicId);
        clinic.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        clinic.setReviewCount((int) count);
        clinicRepo.save(clinic);

        return saved;
    }
}
