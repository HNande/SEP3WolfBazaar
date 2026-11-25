package dk.via.sep3.service;

import dk.via.sep3.dao.ItemListingDAO;
import dk.via.sep3.dao.ItemSummaryDAO;
import dk.via.sep3.dao.ItemSummarySearch;
import dk.via.sep3.dto.ItemListingDataDTO;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.generated.UpdateItemListingRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import dk.via.sep3.util.ListingCategories;

@Service
public class DataItemListingService {

    public static final int MAX_THUMBNAIL_IDS = 50;

    private final ItemListingDAO itemListingDAO;
    private final ItemSummaryDAO itemSummaryDAO;
    private final ItemSummarySearch itemSummarySearch;

    public DataItemListingService(
            ItemListingDAO itemListingDAO,
            ItemSummaryDAO itemSummaryDAO,
            ItemSummarySearch itemSummarySearch) {
        this.itemListingDAO = itemListingDAO;
        this.itemSummaryDAO = itemSummaryDAO;
        this.itemSummarySearch = itemSummarySearch;
    }

    public ItemListingResponseDTO addItem(ItemListingDataDTO dto) {
        if (dto.album() == null || dto.album().images() == null || dto.album().images().isEmpty()) {
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

    public List<ItemSummaryDataDTO> searchItemSummaries(
            String title, List<String> categoryNames, Pageable page) {
        return itemSummarySearch.search(title, categoryNames, page);
    }

    public boolean updateItem(UpdateItemListingRequest request) {
        return itemListingDAO.updateItemListing(
                request.getItemId(),
                request.getItemName(),
                request.getDescription(),
                request.getPrice(),
                ListingCategories.names(request.getCategoriesList()));
    }
}
