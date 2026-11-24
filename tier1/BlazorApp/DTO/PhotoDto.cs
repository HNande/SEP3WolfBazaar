namespace BlazorApp.DTO;

public class PhotoDto
{
    public string Name { get; set; } = string.Empty;
    public int Width { get; set; }
    public int Height { get; set; }
    public PhotoFormat Filetype { get; set; } = PhotoFormat.UNRECOGNIZED;
    public byte[] Data { get; set; } = Array.Empty<byte>();
}