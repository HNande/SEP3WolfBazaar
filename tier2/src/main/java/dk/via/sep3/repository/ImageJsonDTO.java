package dk.via.sep3.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageJsonDTO(
    @NotBlank String name,
    @NotNull ImageFileTypeJsonDTO filetype,
    @Positive int width,
    @Positive int height,
    @NotNull byte[] data
) {
}
