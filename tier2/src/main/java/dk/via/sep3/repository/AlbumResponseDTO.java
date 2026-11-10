package dk.via.sep3.repository;

import java.util.List;

public record AlbumResponseDTO(
    String title,
    List<ImageResponseDTO> images
) {
}
