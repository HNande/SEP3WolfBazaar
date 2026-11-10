package dk.via.sep3.dto;

import dk.via.sep3.entities.ImageFileType;

public record ThumbnailDataDTO(
        int itemId,
        ImageFileType fileType,
        int width,
        int height,
        byte[] bytes
) {}
