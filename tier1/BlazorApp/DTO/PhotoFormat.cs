using System.Text.Json.Serialization;

namespace BlazorApp.DTO;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum PhotoFormat
{
    UNRECOGNIZED = -1,
    PNG = 0,
    JPG = 1,
    JPEG = 2,
}