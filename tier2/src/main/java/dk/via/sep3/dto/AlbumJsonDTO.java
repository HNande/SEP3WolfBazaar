package dk.via.sep3.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AlbumJsonDTO(
    @NotBlank String title,
    @Valid List<@Valid ImageJsonDTO> images

) {
}
