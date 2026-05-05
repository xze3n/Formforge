package com.formforge.repository;

import com.formforge.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** Returns the most recent {@code limit} messages, oldest first. */
    List<ChatMessage> findTop50ByOrderByTimestampAsc();
}
