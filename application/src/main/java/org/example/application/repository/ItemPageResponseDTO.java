package org.example.application.repository;

import java.util.List;

public record ItemPageResponseDTO(
    List<generated.ItemSummary> items,
    String nextPageToken) {
    public boolean hasNext() {
      return nextPageToken != null && !nextPageToken.isEmpty();
    }
  }
