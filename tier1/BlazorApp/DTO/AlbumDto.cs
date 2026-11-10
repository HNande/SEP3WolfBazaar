using System.Text.Json.Serialization;

namespace BlazorApp.DTO;

public class AlbumDto
{
    public string Title { get; set; }
    [JsonPropertyName("images")]
    public List<PhotoDto> Photos { get; set; } = new();
}