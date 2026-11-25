package dk.via.sep3.dto;

import java.util.List;

public record ConversationDataDTO(
        int id,
        List<MessageDataDTO> messages
) {
}