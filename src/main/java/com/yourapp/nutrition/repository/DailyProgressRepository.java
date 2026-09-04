package com.yourapp.nutrition.repository;

import com.yourapp.nutrition.entity.DailyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyProgressRepository extends JpaRepository<DailyProgress, Long> {
    Optional<DailyProgress> findByUserIdAndLogDate(Long userId, LocalDate logDate);
}
