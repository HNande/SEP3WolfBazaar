package dk.via.sep3.dto;

public record ImageDataDTO(
    String name,
    String filetype,
    int width,
    int height,
    byte[] data
) {
}
