package dk.via.sep3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.via.sep3.generated.ImageFileType;
import jakarta.validation.constraints.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageJsonDTO(
    @NotBlank String name,
    @NotNull ImageFileType filetype,
    @Positive int width,
    @Positive int height,
    @NotNull byte[] data
) {
}
