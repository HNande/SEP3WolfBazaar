package dk.via.sep3.dto;

import java.util.List;

public record AlbumResponseDTO(
    String title,
    List<ImageResponseDTO> images
) {
}
