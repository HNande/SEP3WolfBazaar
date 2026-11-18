package dk.via.sep3.dto;

public record ItemSummaryResponseDTO(
    int id,
    String itemName,
    String description,
    int price,
    int ownerId
) {
}
