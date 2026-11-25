package dk.via.sep3.dto;

import java.time.Instant;

public record MessageDataDTO(
        int id,
        int conversationId,
        int senderId,
        String content,
        Instant createdAt
) {
}