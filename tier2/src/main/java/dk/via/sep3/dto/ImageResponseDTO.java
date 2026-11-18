package dk.via.sep3.dto;

public record ImageResponseDTO(
    String name,
    ImageFileTypeJsonDTO filetype,
    int width,
    int height,
    byte[] data
) {
}
