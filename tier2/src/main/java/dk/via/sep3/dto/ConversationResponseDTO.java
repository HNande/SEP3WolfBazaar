package dk.via.sep3.dto;

import java.util.List;

public record ConversationResponseDTO(
        int id,
        List<MessageResponseDTO> messages
) {
}