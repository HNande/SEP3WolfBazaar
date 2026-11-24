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
        itemDto.ImageList = await BuildImageListAsync(files);

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

    private async Task<List<PhotoDto>> BuildImageListAsync(IEnumerable<IBrowserFile> files)
    {
        var fileList = files?.ToList() ?? new List<IBrowserFile>();

        if (fileList.Count > MaxPictures)
            throw new ArgumentException($"You can upload a maximum of {MaxPictures} pictures.");

        var photos = new List<PhotoDto>();

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

            photos.Add(new PhotoDto
            {
                Name = file.Name,
                Width = image.Width,
                Height = image.Height,
                Filetype = contentType == "image/png" ? "PNG" : "JPG",
                Data = fileBytes
            });
        }

        return photos;
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

        if (filetype.Contains("PNG", StringComparison.OrdinalIgnoreCase))
            return "image/png";

        if (filetype.Contains("JPG", StringComparison.OrdinalIgnoreCase) ||
            filetype.Contains("JPEG", StringComparison.OrdinalIgnoreCase))
            return "image/jpeg";

        return null;
    }
    
    public async Task DeleteItemAsync(int id)
    {
        var response = await _http.DeleteAsync($"api/item/{id}");

        if (response.StatusCode == HttpStatusCode.NotFound)
            throw new KeyNotFoundException($"Item with ID {id} was not found.");
        if (!response.IsSuccessStatusCode)
            throw new Exception(await ReadErrorAsync(response));
    }
    
    public async Task UpdateItemAsync(ItemDto itemDto, IEnumerable<IBrowserFile> newFiles)
    {
        var existingPhotos = itemDto.ImageList ?? new List<PhotoDto>();
        var fileList = newFiles?.ToList() ?? new List<IBrowserFile>();

        if (existingPhotos.Count + fileList.Count > MaxPictures)
            throw new ArgumentException($"You can have a maximum of {MaxPictures} pictures in total.");
        
        if (itemDto.Categories.Count == 0)
            throw new ArgumentException("At least one category must be selected.");

        if (existingPhotos.Count + fileList.Count == 0)
            throw new ArgumentException("Item must have at least one picture.");

        var newPhotos = await BuildImageListAsync(fileList);

        itemDto.ImageList = existingPhotos.Concat(newPhotos).ToList();

        var response = await _http.PutAsJsonAsync($"api/item/{itemDto.Id}", itemDto);
        if (!response.IsSuccessStatusCode)
            throw new Exception(await ReadErrorAsync(response));
    }
    
    public async Task<IEnumerable<ItemSummaryDto>> GetItemSummariesAsync(ItemSearchCriteria criteria)
    {
        var query = new List<string>
        {
            $"orderByPrice={criteria.OrderByPrice}",
            $"orderByDate={criteria.OrderByDate}",
            $"counter={criteria.Counter}"
        };

        if (!string.IsNullOrWhiteSpace(criteria.Title))
            query.Add($"title={Uri.EscapeDataString(criteria.Title.Trim())}");

        foreach (var category in criteria.Categories.Where(c => c != Category.CategoryUnspecified).Distinct())
            query.Add($"categories={JsonSerializer.Serialize(category).Trim('"')}");

        var response = await _http.GetAsync($"api/item?{string.Join("&", query)}");
        if (!response.IsSuccessStatusCode)
            throw new Exception(await ReadErrorAsync(response));

        return await response.Content.ReadFromJsonAsync<IEnumerable<ItemSummaryDto>>()
               ?? Enumerable.Empty<ItemSummaryDto>();
    }
}