using BlazorApp.DTO;
using Microsoft.AspNetCore.Components.Forms;
using SixLabors.ImageSharp;

namespace BlazorApp.Services;

public class LocalItemService : IItemService
{
    private const int MaxPictures = 4;
    private const long MaxFileBytes = 10 * 1024 * 1024;
    private readonly object _lock = new();
    private readonly List<StoredItem> _items = new();
    private int _nextId = 1;

    private class StoredItem
    {
        public int Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public int Price { get; set; }
        public int OwnerId { get; set; }
        public List<PhotoDto> Photos { get; set; } = new();
    }

    public LocalItemService()
    {
        _items.Add(new StoredItem
        {
            Id = _nextId++,
            Name = "My Test Laptop",
            Description = "Owned by test user (Id = 1). You can edit or delete this item.",
            Price = 500,
            OwnerId = 1,
            Photos = new List<PhotoDto>()
        });

        _items.Add(new StoredItem
        {
            Id = _nextId++,
            Name = "Someone Else's Bike",
            Description = "Owned by user 8800. You should NOT see Delete or Update buttons here.",
            Price = 1200,
            OwnerId = 8800,
            Photos = new List<PhotoDto>()
        });
        
        _items.Add(new StoredItem
        {
            Id = _nextId++,
            Name = "Someone Else's Bike",
            Description = "Owned by user 8800. You should NOT see Delete or Update buttons here.",
            Price = 1200,
            OwnerId = 8800,
            Photos = new List<PhotoDto>()
        });
        
        _items.Add(new StoredItem
        {
            Id = _nextId++,
            Name = "Someone Else's Bike",
            Description = "Owned by user 8800. You should NOT see Delete or Update buttons here.",
            Price = 1200,
            OwnerId = 8800,
            Photos = new List<PhotoDto>()
        });
        
        _items.Add(new StoredItem
        {
            Id = _nextId++,
            Name = "Someone Else's Bike",
            Description = "Owned by user 8800. You should NOT see Delete or Update buttons here.",
            Price = 1200,
            OwnerId = 8800,
            Photos = new List<PhotoDto>()
        });
        
        _items.Add(new StoredItem
        {
            Id = _nextId++,
            Name = "Someone Else's Bike",
            Description = "Owned by user 8800. You should NOT see Delete or Update buttons here.",
            Price = 1200,
            OwnerId = 8800,
            Photos = new List<PhotoDto>()
        });
    }

    public async Task CreateItemAsync(ItemDto itemDto, IEnumerable<IBrowserFile> files)
    {
        var photos = await ConvertFilesToPhotosAsync(files);

        lock (_lock)
        {
            _items.Add(new StoredItem
            {
                Id = _nextId++,
                Name = itemDto.Name,
                Description = itemDto.Description,
                Price = itemDto.Price ?? 0,
                OwnerId = itemDto.OwnerId,
                Photos = photos
            });
        }
    }

    public async Task UpdateItemAsync(ItemDto itemDto, IEnumerable<IBrowserFile> newFiles)
    {
        var existingPhotos = itemDto.ImageList ?? new List<PhotoDto>();
        var addedPhotos = await ConvertFilesToPhotosAsync(newFiles);

        if (existingPhotos.Count + addedPhotos.Count > MaxPictures)
            throw new ArgumentException($"You can have a maximum of {MaxPictures} pictures in total.");

        var combinedPhotos = existingPhotos.Concat(addedPhotos).ToList();

        lock (_lock)
        {
            var item = _items.FirstOrDefault(i => i.Id == itemDto.Id)
                       ?? throw new KeyNotFoundException($"Item with ID {itemDto.Id} was not found.");

            item.Name = itemDto.Name;
            item.Description = itemDto.Description;
            item.Price = itemDto.Price ?? 0;
            item.Photos = combinedPhotos;
        }
    }

    public Task<ItemDto> GetItemByIdAsync(int id)
    {
        lock (_lock)
        {
            var item = _items.FirstOrDefault(i => i.Id == id)
                       ?? throw new KeyNotFoundException($"Item with ID {id} was not found.");

            return Task.FromResult(new ItemDto
            {
                Id = item.Id,
                Name = item.Name,
                Description = item.Description,
                Price = item.Price,
                OwnerId = item.OwnerId,
                ImageList = item.Photos.Select(p => new PhotoDto
                {
                    Name = p.Name,
                    Width = p.Width,
                    Height = p.Height,
                    Filetype = p.Filetype,
                    Data = p.Data
                }).ToList()
            });
        }
    }

    public Task<IEnumerable<ItemSummaryDto>> GetItemSummariesAsync()
    {
        lock (_lock)
        {
            var summaries = _items.Select(i => new ItemSummaryDto
            {
                Id = i.Id,
                Name = i.Name,
                Description = i.Description,
                Price = i.Price,
                OwnerId = i.OwnerId
            }).ToList();

            return Task.FromResult<IEnumerable<ItemSummaryDto>>(summaries);
        }
    }

    public Task<Dictionary<int, string>> GetThumbnailsAsync(IEnumerable<int> ids)
    {
        var idSet = ids.ToHashSet();
        lock (_lock)
        {
            var result = new Dictionary<int, string>();
            foreach (var item in _items.Where(i => idSet.Contains(i.Id)))
            {
                var first = item.Photos.FirstOrDefault();
                if (first?.Data is { Length: > 0 })
                {
                    var mime = first.Filetype.Equals("PNG", StringComparison.OrdinalIgnoreCase)
                        ? "image/png"
                        : "image/jpeg";
                    result[item.Id] = $"data:{mime};base64,{Convert.ToBase64String(first.Data)}";
                }
            }

            return Task.FromResult(result);
        }
    }

    public Task DeleteItemAsync(int id)
    {
        lock (_lock)
        {
            var removed = _items.RemoveAll(i => i.Id == id);
            if (removed == 0)
                throw new KeyNotFoundException($"Item with ID {id} was not found.");
        }

        return Task.CompletedTask;
    }

    private static async Task<List<PhotoDto>> ConvertFilesToPhotosAsync(IEnumerable<IBrowserFile>? files)
    {
        var result = new List<PhotoDto>();
        if (files == null) return result;

        foreach (var file in files)
        {
            using var stream = file.OpenReadStream(maxAllowedSize: MaxFileBytes);
            using var ms = new MemoryStream();
            await stream.CopyToAsync(ms);
            var bytes = ms.ToArray();

            ms.Position = 0;
            using var image = Image.Load(ms);

            result.Add(new PhotoDto
            {
                Name = file.Name,
                Width = image.Width,
                Height = image.Height,
                Filetype = file.ContentType.Contains("png", StringComparison.OrdinalIgnoreCase) ? "PNG" : "JPG",
                Data = bytes
            });
        }

        return result;
    }
}