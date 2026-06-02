package com.formforge.repository;

import com.formforge.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Returns the most recent {@code limit} messages, oldest first. */
    List<ChatMessage> findTop50ByOrderByTimestampAsc();
}
