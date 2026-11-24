namespace BlazorApp.DTO;

public record ThumbnailDto(
    int ItemId,
    PhotoFormat Filetype,
    string Data);