namespace BlazorApp.DTO;

public class PhotoDto
{
    public string Name { get; set; } = string.Empty;
    public int Width { get; set; }
    public int Height { get; set; }
    public string Filetype { get; set; } = string.Empty;
    public byte[] Data { get; set; } = Array.Empty<byte>();
}