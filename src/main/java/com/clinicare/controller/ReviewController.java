package com.clinicare.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicare.model.Review;
import com.clinicare.repository.UserRepository;
import com.clinicare.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepo;

    /** GET /api/reviews/clinic/{clinicId} */
    @GetMapping("/clinic/{clinicId}")
    public ResponseEntity<?> getReviews(@PathVariable Long clinicId) {
        List<Review> reviews = reviewService.getClinicReviews(clinicId);
        return ResponseEntity.ok(reviews.stream().map(r -> Map.of(
                "id", r.getId(),
                "reviewerName", r.getReviewerName() != null ? r.getReviewerName() : "Anonymous",
                "rating", r.getRating(),
                "comment", r.getComment() != null ? r.getComment() : "",
                "createdAt", r.getCreatedAt().toString()
        )).collect(Collectors.toList()));
    }

    
    @PostMapping
    public ResponseEntity<?> addReview(
            @Valid @RequestBody ReviewRequest req,
            Authentication auth  // nullable — guest has no auth
    ) {
        try {
            Long userId = null;
            if (auth != null) {
                var user = userRepo.findByEmail(auth.getName()).orElse(null);
                if (user != null) userId = user.getId();
            }
            Review review = reviewService.addOrUpdateReview(
                    req.clinicId, userId, req.reviewerName, req.rating, req.comment
            );
            return ResponseEntity.ok(Map.of(
                    "id", review.getId(),
                    "message", "Review submitted!"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    record ReviewRequest(
            @NotNull Long clinicId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 1000) String comment,
            String reviewerName
    ) {}
}
