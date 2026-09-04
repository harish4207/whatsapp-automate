package com.yourapp.onboarding.repository;

import com.yourapp.onboarding.entity.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {

    Optional<ConversationState> findByPhoneNumber(String phoneNumber);

    Optional<ConversationState> findByUserId(Long userId);
}
