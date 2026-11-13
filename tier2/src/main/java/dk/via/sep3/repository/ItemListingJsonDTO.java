package dk.via.sep3.repository;

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

    //In uc description we say a price value of 0 is considered "free", and is considered valid
    //apparently Dec rejects free items, so min is a better choice
    @NotNull @Min(value = 0, message = "Price cannot be negative")
    int price,

    @Valid List<ImageJsonDTO> imageList,

    @NotNull
    int ownerId
) {

}
