using BlazorApp.DTO;
using Microsoft.AspNetCore.Components.Forms;

namespace BlazorApp.Services;

public interface IItemService
{
    Task CreateItemAsync(ItemDto itemDto, IEnumerable<IBrowserFile> files);
    Task<ItemDto> GetItemByIdAsync(int id);
    Task<IEnumerable<ItemSummaryDto>> GetItemSummariesAsync();
    Task<Dictionary<int, string>> GetThumbnailsAsync(IEnumerable<int> ids);
    Task DeleteItemAsync(int id);
}