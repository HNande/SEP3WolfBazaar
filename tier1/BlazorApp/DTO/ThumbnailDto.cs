namespace BlazorApp.DTO;

public record ThumbnailDto(
    int ItemId,
    string Filetype,
    int Width,
    int Height,
    string Data);