package dk.via.sep3.repository;

public record ImageResponseDTO(
    String name,
    ImageFileTypeJsonDTO filetype,
    int width,
    int height,
    byte[] data
) {
}
