package org.example.application.controller;

import jakarta.validation.Valid;
import org.example.application.repository.ItemListingJsonDTO;
import org.example.application.repository.ItemListingResponseDTO;
import org.example.application.repository.ItemSummaryResponseDTO;
import org.example.application.repository.ThumbnailResponseDTO;
import org.example.application.service.ItemListingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
