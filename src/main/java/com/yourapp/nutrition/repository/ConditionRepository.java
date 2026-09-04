package com.yourapp.nutrition.repository;

import com.yourapp.nutrition.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, Long> {
    Optional<Condition> findByNameIgnoreCase(String name);
}
