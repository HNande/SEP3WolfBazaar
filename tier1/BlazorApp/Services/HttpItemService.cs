using System.Net;
using System.Text.Json;
using BlazorApp.DTO;
using Microsoft.AspNetCore.Components.Forms;
using SixLabors.ImageSharp;

namespace BlazorApp.Services;

public class HttpItemService : IItemService
{
    private const int MaxPictures = 4;
    private const long MaxFileBytes = 10 * 1024 * 1024;
    private const int MaxDimension = 2048;

    private readonly HttpClient _http;

    public HttpItemService(HttpClient httpClient)
    {
        _http = httpClient;
    }

    public async Task CreateItemAsync(ItemDto itemDto, IEnumerable<IBrowserFile> files)
    {
        itemDto.Album = await BuildAlbumAsync(itemDto.Name, files);

        var response = await _http.PostAsJsonAsync("api/item", itemDto);
        if (!response.IsSuccessStatusCode)
            throw new Exception(await ReadErrorAsync(response));
    }

    public async Task<ItemDto> GetItemByIdAsync(int id)
    {
        var response = await _http.GetAsync($"api/item/{id}");

        if (response.StatusCode == HttpStatusCode.NotFound)
            throw new KeyNotFoundException($"Item with ID {id} was not found.");
        if (!response.IsSuccessStatusCode)
            throw new Exception(await ReadErrorAsync(response));

        return await response.Content.ReadFromJsonAsync<ItemDto>()
               ?? throw new InvalidOperationException("Failed to deserialize item response.");
    }

    public async Task<IEnumerable<ItemSummaryDto>> GetItemSummariesAsync()
    {
        var response = await _http.GetAsync("api/item");
        if (!response.IsSuccessStatusCode)
            throw new Exception(await ReadErrorAsync(response));

        return await response.Content.ReadFromJsonAsync<IEnumerable<ItemSummaryDto>>()
               ?? Enumerable.Empty<ItemSummaryDto>();
    }

    public async Task<Dictionary<int, string>> GetThumbnailsAsync(IEnumerable<int> ids)
    {
        var idList = ids.Distinct().ToList();
        var result = new Dictionary<int, string>();
        if (idList.Count == 0)
            return result;

        var url = $"api/item/thumbnails?ids={string.Join(",", idList)}";
        var thumbnails = await _http.GetFromJsonAsync<List<ThumbnailDto>>(url)
                         ?? new List<ThumbnailDto>();

        foreach (var t in thumbnails)
        {
            var mime = ToMimeType(t.Filetype);
            if (mime is null || string.IsNullOrEmpty(t.Data))
                continue;

            result[t.ItemId] = $"data:{mime};base64,{t.Data}";
        }

        return result;
    }

    private async Task<AlbumDto> BuildAlbumAsync(string itemName, IEnumerable<IBrowserFile> files)
    {
        var fileList = files?.ToList() ?? new List<IBrowserFile>();

        if (fileList.Count > MaxPictures)
            throw new ArgumentException($"You can upload a maximum of {MaxPictures} pictures.");

        var album = new AlbumDto
        {
            Title = $"{itemName} album",
            Photos = new List<PhotoDto>()
        };

        foreach (var file in fileList)
        {
            var contentType = file.ContentType?.ToLowerInvariant();
            if (contentType is not ("image/png" or "image/jpeg" or "image/jpg"))
                throw new ArgumentException($"File {file.Name} must be in PNG or JPEG format.");

            using var stream = file.OpenReadStream(maxAllowedSize: MaxFileBytes);
            using var memoryStream = new MemoryStream();
            await stream.CopyToAsync(memoryStream);

            var fileBytes = memoryStream.ToArray();

            memoryStream.Position = 0;
            using var image = Image.Load(memoryStream);

            if (image.Width <= 0 || image.Height <= 0 || image.Width > MaxDimension || image.Height > MaxDimension)
                throw new ArgumentException(
                    $"Picture {file.Name} resolution must be up to {MaxDimension}x{MaxDimension} pixels. Current: {image.Width}x{image.Height}");

            album.Photos.Add(new PhotoDto
            {
                Name = file.Name,
                Width = image.Width,
                Height = image.Height,
                Filetype = contentType == "image/png" ? "PNG" : "JPG",
                Data = fileBytes
            });
        }

        return album;
    }

    private static async Task<string> ReadErrorAsync(HttpResponseMessage response)
    {
        try
        {
            var error = await response.Content.ReadFromJsonAsync<ApiErrorDto>();
            if (!string.IsNullOrWhiteSpace(error?.Message))
                return error.Message;
        }
        catch (Exception ex) when (ex is JsonException or NotSupportedException)
        {
        }

        return $"Request failed (HTTP {(int)response.StatusCode} {response.ReasonPhrase})";
    }

    private static string? ToMimeType(string? filetype)
    {
        if (string.IsNullOrWhiteSpace(filetype))
            return null;

        var value = filetype.Trim().TrimStart('.').ToLowerInvariant();

        return value switch
        {
            "png" => "image/png",
            "jpg" or "jpeg" => "image/jpeg",
            _ => null
        };
    }
    
    public async Task DeleteItemAsync(int id)
    {
        var response = await _http.DeleteAsync($"api/item/{id}");

        if (response.StatusCode == HttpStatusCode.NotFound)
            throw new KeyNotFoundException($"Item with ID {id} was not found.");
        if (!response.IsSuccessStatusCode)
            throw new Exception(await ReadErrorAsync(response));
    }
}