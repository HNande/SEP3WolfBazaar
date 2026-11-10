package dk.via.sep3.repository;

import dk.via.sep3.generated.ItemSummary;

import java.util.List;

public record ItemPageResponseDTO(
    List<ItemSummary> items,
    String nextPageToken) {
    public boolean hasNext() {
      return nextPageToken != null && !nextPageToken.isEmpty();
    }
  }
