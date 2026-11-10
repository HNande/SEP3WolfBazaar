package dk.via.sep3.dto;

public record ItemListingDataDTO(
    String itemName,
    String description,
    int price,
    AlbumDataDTO album
){
}
