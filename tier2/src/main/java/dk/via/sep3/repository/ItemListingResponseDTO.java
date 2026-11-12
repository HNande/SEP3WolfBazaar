package dk.via.sep3.repository;

import dk.via.sep3.generated.Category;

import java.util.List;

public record ItemListingResponseDTO(
    int id,
    String itemName,
    String description,
    int price,
    List<ImageResponseDTO> imageList,
    List<Category> categoryList,
    int year,
    int month,
    int day,
    int ownerId
    ){
}
