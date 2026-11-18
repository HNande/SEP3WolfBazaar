package dk.via.sep3.service;

import dk.via.sep3.client.ItemListingGrpcClient;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.dto.ItemListingJsonDTO;
import dk.via.sep3.mapper.ItemListingMapper;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSummaryResponseDTO;
import dk.via.sep3.dto.ThumbnailResponseDTO;
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

  public void updateItem(int id, ItemListingJsonDTO dto) {
    if (id <= 0) {
      throw new InvalidRequestException("Item id must be a positive integer");
    }
    if (!client.updateItem(id, dto)) {
      throw new NoSuchElementException("Item " + id + " not found");
    }
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
