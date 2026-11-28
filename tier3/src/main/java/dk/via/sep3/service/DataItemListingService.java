package dk.via.sep3.service;

import dk.via.sep3.dao.ItemListingDAO;
import dk.via.sep3.dao.ItemSummaryDAO;
import dk.via.sep3.dao.ItemSummarySearch;
import dk.via.sep3.dto.ItemListingDataDTO;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.dto.UpdateItemListingDataDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DataItemListingService {

    public static final int MAX_THUMBNAIL_IDS = 50;

    private final ItemListingDAO itemListingDAO;
    private final ItemSummaryDAO itemSummaryDAO;
    private final Optional<ItemSummarySearch> itemSummarySearch;

    public DataItemListingService(ItemListingDAO itemListingDAO, ItemSummaryDAO itemSummaryDAO,
                                  Optional<ItemSummarySearch> itemSummarySearch) {
        this.itemListingDAO = itemListingDAO;
        this.itemSummaryDAO = itemSummaryDAO;
        this.itemSummarySearch = itemSummarySearch;
    }

    public ItemListingResponseDTO addItem(ItemListingDataDTO dto) {
        if (dto.images() == null || dto.images().isEmpty()) {
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

    public Optional<List<ItemSummaryDataDTO>> searchItemSummaries(
            String title, List<String> categoryNames, Pageable page) {
        return itemSummarySearch.map(search -> search.search(title, categoryNames, page));
    }

    public List<ThumbnailDataDTO> getFirstThumbnails(List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_THUMBNAIL_IDS) {
            throw new IllegalArgumentException(
                    "Between 1 and " + MAX_THUMBNAIL_IDS + " item ids are required");
        }
        return itemSummaryDAO.getFirstThumbnails(ids.stream().distinct().toList());
    }

    public boolean updateItem(UpdateItemListingDataDTO dto) {
        if (dto.price() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        return itemListingDAO.updateItemListing(dto);
    }
}
