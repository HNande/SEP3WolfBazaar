package dk.via.sep3.dto;

import java.util.List;

public record AlbumDataDTO(
    String title,
    List<ImageDataDTO> images
) {
}
