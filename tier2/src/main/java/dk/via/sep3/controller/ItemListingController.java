package dk.via.sep3.controller;

import dk.via.sep3.dto.*;
import dk.via.sep3.generated.Category;
import jakarta.validation.Valid;
import dk.via.sep3.service.ItemListingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemListingController {

  private final ItemListingService itemListingService;

  public ItemListingController(ItemListingService itemListingService) {
    this.itemListingService = itemListingService;
  }

  @GetMapping("")
  public List<ItemSummaryResponseDTO> getItemSummaries(
          @RequestParam(required = false) String title,
          @RequestParam(required = false) List<Category> categories,
          @RequestParam(defaultValue = "0") int orderByPrice,
          @RequestParam(defaultValue = "0") int orderByDate,
          @RequestParam(defaultValue = "1") int counter) {
    return itemListingService.getItemSummaries(
            new ItemSearchCriteriaDTO(title, categories, orderByPrice, orderByDate, counter));
  }

  @GetMapping("/thumbnails")
  public List<ThumbnailResponseDTO> getThumbnails(@RequestParam("ids") List<Integer> ids) {
    return itemListingService.getThumbnails(ids);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void addItem(@Valid @RequestBody ItemListingJsonDTO form) {
    itemListingService.addItem(form); // throws Status code 201 upon success
  }

  @GetMapping("/{id}")
  public ItemListingResponseDTO getItemListingId(@PathVariable int id) {
    return itemListingService.getItemById(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteItemListing(@PathVariable int id) {
    itemListingService.removeItem(id);     // throws Status code 404 on not found
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateItem(@PathVariable int id, @Valid @RequestBody ItemListingJsonDTO form) {
    itemListingService.updateItem(id, form);
  }

}
