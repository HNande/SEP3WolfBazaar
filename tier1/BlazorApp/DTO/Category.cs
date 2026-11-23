using System.Text.Json.Serialization;

namespace BlazorApp.DTO;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum Category
{
    CategoryUnspecified = 0,
    CategoryClothing = 1,
    CategoryAccessories = 2,
    CategoryCurios = 3
}