using System.Text.Json.Serialization;

namespace BlazorApp.DTO;

public class ItemSummaryDto
{
    public int Id { get; set; }

    [JsonPropertyName("itemName")]
    public string Name { get; set; } = string.Empty;

    public string? Description { get; set; }

    public int Price { get; set; }

    // [JsonPropertyName("ownerId")]
    // public int OwnerId { get; set; }

    [JsonIgnore]
    public string? ImageUrl { get; set; }
}
