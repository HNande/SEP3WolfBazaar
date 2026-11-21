package dk.via.sep3.dto;

import dk.via.sep3.generated.ImageFileType;

public record ImageResponseDTO(
    String name,
    ImageFileType filetype,
    int width,
    int height,
    byte[] data
) {
}
