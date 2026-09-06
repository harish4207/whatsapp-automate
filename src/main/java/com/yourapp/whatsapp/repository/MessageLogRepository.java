package com.yourapp.whatsapp.repository;

import com.yourapp.whatsapp.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    boolean existsByMessageId(String messageId);

    Optional<MessageLog> findByMessageId(String messageId);

    java.util.List<MessageLog> findTop10ByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}
