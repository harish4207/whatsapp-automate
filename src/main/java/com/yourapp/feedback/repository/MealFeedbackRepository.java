package com.yourapp.feedback.repository;

import com.yourapp.feedback.entity.MealFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealFeedbackRepository extends JpaRepository<MealFeedback, Long> {

    List<MealFeedback> findByUserId(Long userId);
}
