package dk.via.sep3.dto;

import java.util.List;

/**
 * Data for updating an existing listing. Blank text fields and empty lists mean "keep the current value";
 * a non-empty image list replaces all existing pictures of the listing.
 */
public record UpdateItemListingDataDTO(
        int itemId,
        String itemName,
        String description,
        int price,
        List<ImageDataDTO> images,
        List<String> categoryNames
) {}
