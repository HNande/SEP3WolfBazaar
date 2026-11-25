package dk.via.sep3.dto;

public record ConversationSummaryResponseDTO(
        int id,
        int ownerId,
        String itemName
) {
}