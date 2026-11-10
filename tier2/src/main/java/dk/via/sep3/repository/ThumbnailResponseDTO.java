package dk.via.sep3.repository;

public record ThumbnailResponseDTO(
    int itemId,
    ImageFileTypeJsonDTO filetype,
    int width,
    int height,
    byte[] data
) {
}
