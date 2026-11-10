package dk.via.sep3.controller;

import jakarta.validation.Valid;
import dk.via.sep3.repository.ItemListingJsonDTO;
import dk.via.sep3.repository.ItemListingResponseDTO;
import dk.via.sep3.repository.ItemSummaryResponseDTO;
import dk.via.sep3.repository.ThumbnailResponseDTO;
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
  public List<ItemSummaryResponseDTO> getItemSummaries() {
    return itemListingService.getItemSummaries();
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
}
