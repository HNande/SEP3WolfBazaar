package dk.via.sep3.dto;

public record ThumbnailResponseDTO(
    int itemId,
    ImageFileTypeJsonDTO filetype,
    int width,
    int height,
    byte[] data
) {
}
