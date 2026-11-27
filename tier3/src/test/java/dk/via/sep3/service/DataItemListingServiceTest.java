package dk.via.sep3.service;

import dk.via.sep3.dao.ItemListingDAO;
import dk.via.sep3.dao.ItemSummaryDAO;
import dk.via.sep3.dao.ItemSummarySearch;
import dk.via.sep3.dto.ImageDataDTO;
import dk.via.sep3.dto.ItemListingDataDTO;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.dto.UpdateItemListingDataDTO;
import dk.via.sep3.entities.ImageFileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataItemListingServiceTest {

    private ItemListingDAO itemListingDAO;
    private ItemSummaryDAO itemSummaryDAO;
    private ItemSummarySearch itemSummarySearch;
    private DataItemListingService service;

    @BeforeEach
    void setUp() {
        itemListingDAO = mock(ItemListingDAO.class);
        itemSummaryDAO = mock(ItemSummaryDAO.class);
        itemSummarySearch = mock(ItemSummarySearch.class);
        service = new DataItemListingService(itemListingDAO, itemSummaryDAO, Optional.of(itemSummarySearch));
    }

    @Test
    void addItem_requiresPicturesBeforeDelegating() {
        assertThrows(IllegalArgumentException.class, () -> service.addItem(
                new ItemListingDataDTO("Coat", "Warm", 20, List.of(), 2, List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.addItem(
                new ItemListingDataDTO("Coat", "Warm", 20, null, 2, List.of())));
        verifyNoInteractions(itemListingDAO);
    }

    @Test
    void addItemDelegatesListingWithPictures() {
        ItemListingDataDTO listing = new ItemListingDataDTO(
                "Coat", "Warm", 20, List.of(new ImageDataDTO("coat.png", "PNG", 1, 1, new byte[]{1})),
                2, List.of("CLOTHING"));
        ItemListingResponseDTO expected = response(8);
        when(itemListingDAO.addNewItemListing(listing)).thenReturn(expected);

        assertEquals(expected, service.addItem(listing));
        verify(itemListingDAO).addNewItemListing(listing);
    }

    @Test
    void removeAndGetItemDelegateToDao() {
        ItemListingResponseDTO expected = response(8);
        when(itemListingDAO.getItemById(8)).thenReturn(Optional.of(expected));
        when(itemListingDAO.removeItemListing(8)).thenReturn(true);

        assertEquals(Optional.of(expected), service.getItemById(8));
        assertTrue(service.removeItem(8));
        verify(itemListingDAO).getItemById(8);
        verify(itemListingDAO).removeItemListing(8);
    }

    @Test
    void searchDelegatesCriteriaAndPagingWhenSearchIsConfigured() {
        Pageable page = PageRequest.of(1, 10);
        List<ItemSummaryDataDTO> expected = List.of(
                new ItemSummaryDataDTO(3, "Coat", "Warm", 20, LocalDate.of(2025, 1, 1), 2));
        when(itemSummarySearch.search("coat", List.of("CLOTHING"), page)).thenReturn(expected);

        assertEquals(Optional.of(expected), service.searchItemSummaries("coat", List.of("CLOTHING"), page));
        verify(itemSummarySearch).search("coat", List.of("CLOTHING"), page);
    }

    @Test
    void searchReturnsEmptyWhenNoSearchIsConfigured() {
        DataItemListingService noSearch = new DataItemListingService(
                itemListingDAO, itemSummaryDAO, Optional.empty());

        assertEquals(Optional.empty(), noSearch.searchItemSummaries("", List.of(), PageRequest.of(0, 10)));
    }

    @Test
    void getFirstThumbnailsDeduplicatesIdsAndEnforcesSizeBounds() {
        List<Integer> ids = List.of(3, 3, 5);
        List<ThumbnailDataDTO> expected = List.of(new ThumbnailDataDTO(3, ImageFileType.PNG, 1, 1, new byte[]{1}));
        when(itemSummaryDAO.getFirstThumbnails(List.of(3, 5))).thenReturn(expected);

        assertEquals(expected, service.getFirstThumbnails(ids));
        verify(itemSummaryDAO).getFirstThumbnails(List.of(3, 5));

        assertThrows(IllegalArgumentException.class, () -> service.getFirstThumbnails(null));
        assertThrows(IllegalArgumentException.class, () -> service.getFirstThumbnails(List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.getFirstThumbnails(
                java.util.stream.IntStream.range(0, DataItemListingService.MAX_THUMBNAIL_IDS + 1)
                        .boxed().toList()));
    }

    @Test
    void updateRejectsNegativePriceAndDelegatesValidUpdate() {
        UpdateItemListingDataDTO invalid = new UpdateItemListingDataDTO(8, "Coat", "Warm", -1, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.updateItem(invalid));

        UpdateItemListingDataDTO valid = new UpdateItemListingDataDTO(8, "Coat", "Warm", 20, null, null);
        when(itemListingDAO.updateItemListing(valid)).thenReturn(true);

        assertTrue(service.updateItem(valid));
        verify(itemListingDAO).updateItemListing(valid);
    }

    @Test
    void removeReturnsDaoResult() {
        when(itemListingDAO.removeItemListing(99)).thenReturn(false);

        assertFalse(service.removeItem(99));
    }

    private static ItemListingResponseDTO response(int id) {
        return new ItemListingResponseDTO(id, "Coat", "Warm", 20, List.of(), List.of(), 2025, 1, 1, 2);
    }
}