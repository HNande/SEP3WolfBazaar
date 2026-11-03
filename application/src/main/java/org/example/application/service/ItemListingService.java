package org.example.application.service;

import generated.AddItemListingRequest;
import org.example.application.client.ItemListingGrpcClient;
import org.example.application.exception.InvalidRequestException;
import org.example.application.repository.ItemListingJsonDTO;
import org.example.application.repository.ItemListingMapper;
import org.example.application.repository.ItemListingResponseDTO;
import org.example.application.repository.ItemSummaryResponseDTO;
import org.example.application.repository.ThumbnailResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ItemListingService {

  public static final int MAX_THUMBNAIL_IDS = 50;

  private final ItemListingGrpcClient client;

  public ItemListingService(ItemListingGrpcClient client) {
    this.client = client;
  }

  public boolean addItem(ItemListingJsonDTO dto) {
    return client.addItem(dto);
  }

  public boolean removeItem(int id){
    return client.removeItem(id);
  }

  public ItemListingResponseDTO getItemById(int id) {
    return client.getItemById(id)
            .map(ItemListingMapper::toResponse)
            .orElseThrow(() -> new NoSuchElementException("Item " + id + " not found"));
  }

  public List<ItemSummaryResponseDTO> getItemSummaries() {
    return client.getAllItemSummaries().stream()
            .map(ItemListingMapper::toSummaryResponse)
            .toList();
  }

  public List<ThumbnailResponseDTO> getThumbnails(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      throw new InvalidRequestException("At least one item id is required");
    }
    List<Integer> distinct = ids.stream().distinct().toList();
    if (distinct.size() > MAX_THUMBNAIL_IDS) {
      throw new InvalidRequestException("At most " + MAX_THUMBNAIL_IDS + " item ids are allowed per request");
    }
    if (distinct.stream().anyMatch(id -> id == null || id <= 0)) {
      throw new InvalidRequestException("Item ids must be positive integers");
    }
    return client.getThumbnails(distinct).stream()
            .map(ItemListingMapper::toThumbnailResponse)
            .toList();
  }
}
