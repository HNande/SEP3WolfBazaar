using System.Text.Json.Serialization;

namespace BlazorApp.DTO;

public class AlbumDto
{
    [JsonPropertyName("images")]
    public List<PhotoDto> Photos { get; set; } = new();
}