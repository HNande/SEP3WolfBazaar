package dk.via.sep3.dto;

import java.util.List;

public record ItemListingDataDTO(
        String itemName,
        String description,
        int price,
        List<ImageDataDTO> images,
        int ownerId,
        List<String> categoryNames
) {}
