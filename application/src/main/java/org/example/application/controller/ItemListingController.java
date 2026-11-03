package org.example.application.controller;

import jakarta.validation.Valid;
import org.example.application.repository.ItemListingJsonDTO;
import org.example.application.repository.ItemListingResponseDTO;
import org.example.application.repository.ItemSummaryResponseDTO;
import org.example.application.repository.ThumbnailResponseDTO;
import org.example.application.service.ItemListingService;
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

  @PostMapping("")
  public ResponseEntity<ItemListingResponseDTO> addItem(@Valid @RequestBody ItemListingJsonDTO form) {
    ItemListingResponseDTO saved = itemListingService.addItem(form);

    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.id()).toUri();

    return ResponseEntity.created(location).body(saved);
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
