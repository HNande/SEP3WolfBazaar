package org.example.application.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ItemListingJsonDTO(
    @NotBlank(message = "Item Name is required")
    @Size(min = ItemListingConstraints.ITEM_NAME_MIN, max = ItemListingConstraints.ITEM_NAME_MAX,
        message = "Item name must be between {min} and {max} characters")
    String itemName,

    @NotBlank(message = "Description is required")
    @Size(min = ItemListingConstraints.DESCRIPTION_MIN,max = ItemListingConstraints.DESCRIPTION_MAX,
        message = "Description must be between {min} and {max} characters")
     String description,

    @Positive(message = "Price must be greater than 0")
    int price,
     @Valid List<ImageJsonDTO> imageList
) {

}
