namespace BlazorApp.DTO;

public class ItemSearchCriteria
{
    public string? Title { get; set; }
    public List<Category> Categories { get; set; } = new();
    public int OrderByPrice { get; set; }
    public int OrderByDate { get; set; }
    public int Counter { get; set; } = 1;
}