package dk.via.sep3.dto;

import java.time.Instant;

public record MessageResponseDTO(
        int id,
        int conversationId,
        int senderId,
        String content,
        Instant createdAt
) {
}