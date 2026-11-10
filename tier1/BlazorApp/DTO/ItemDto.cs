using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace BlazorApp.DTO;

public class ItemDto
{
    public int Id { get; set; }

    [JsonPropertyName("itemName")]
    [Required(ErrorMessage = "Name is required.")]
    [StringLength(200, MinimumLength = 8, ErrorMessage = "Name must be between 8 and 200 characters.")]
    public string Name { get; set; } = string.Empty;

    [Required(ErrorMessage = "Description is required.")]
    [StringLength(500, MinimumLength = 20, ErrorMessage = "Description must be between 20 and 500 characters.")]
    public string Description { get; set; } = string.Empty;

    [Required(ErrorMessage = "Price is required.")]
    [Range(1, 1000000, ErrorMessage = "Price must be between 1 and 1,000,000.")]
    public int? Price { get; set; }

    public AlbumDto? Album { get; set; }

    // [JsonPropertyName("ownerId")]
    // public int OwnerId { get; set; }
}
