package org.example.data.service;

import org.example.data.dao.ItemListingDAO;
import org.example.data.dao.ItemSummaryDAO;
import org.example.data.dto.ItemListingDataDTO;
import org.example.data.dto.ItemListingResponseDTO;
import org.example.data.dto.ThumbnailDataDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DataItemListingService {

    public static final int MAX_THUMBNAIL_IDS = 50;

    private final ItemListingDAO itemListingDAO;
    private final ItemSummaryDAO itemSummaryDAO;

    public DataItemListingService(ItemListingDAO itemListingDAO, ItemSummaryDAO itemSummaryDAO) {
        this.itemListingDAO = itemListingDAO;
        this.itemSummaryDAO = itemSummaryDAO;
    }

    public ItemListingResponseDTO addItem(ItemListingDataDTO dto) {
        if (dto.album() == null || dto.album().images().isEmpty()) {
            throw new IllegalArgumentException("A listing needs at least one picture");
        }
        return itemListingDAO.addNewItemListing(dto);
    }

    public boolean removeItem(int id)
    {
        return itemListingDAO.removeItemListing(id);
    }

    public Optional<ItemListingResponseDTO> getItemById(int id) {
        return itemListingDAO.getItemById(id);
    }

    public ItemListingDAO.ItemListingPage getItems(int pageSize, String pageToken) {
        return itemListingDAO.getItems(pageSize, pageToken);
    }

    public ItemSummaryDAO.ItemSummaryPage getItemSummaries(int pageSize, String pageToken) {
        return itemSummaryDAO.getItemSummaries(pageSize, pageToken);
    }

    public List<ThumbnailDataDTO> getFirstThumbnails(List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_THUMBNAIL_IDS) {
            throw new IllegalArgumentException(
                    "Between 1 and " + MAX_THUMBNAIL_IDS + " item ids are required");
        }
        return itemSummaryDAO.getFirstThumbnails(ids.stream().distinct().toList());
    }
}
