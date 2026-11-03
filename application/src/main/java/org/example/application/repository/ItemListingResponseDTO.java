package org.example.application.repository;

import generated.Category;

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
    int day
    ){
}
