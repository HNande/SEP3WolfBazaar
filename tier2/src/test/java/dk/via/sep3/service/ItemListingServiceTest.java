package dk.via.sep3.service;

import com.google.protobuf.ByteString;
import dk.via.sep3.client.ItemListingGrpcClient;
import dk.via.sep3.dto.ImageJsonDTO;
import dk.via.sep3.dto.ItemListingJsonDTO;
import dk.via.sep3.dto.ItemSearchCriteriaDTO;
import dk.via.sep3.dto.ItemSummaryResponseDTO;
import dk.via.sep3.dto.ThumbnailResponseDTO;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.generated.Category;
import dk.via.sep3.generated.Date;
import dk.via.sep3.generated.ImageFileType;
import dk.via.sep3.generated.ItemListing;
import dk.via.sep3.generated.ItemSummary;
import dk.via.sep3.generated.ItemThumbnail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemListingServiceTest {

    private ItemListingGrpcClient client;
    private ItemListingService service;

    @BeforeEach
    void setUp() {
        client = mock(ItemListingGrpcClient.class);
        service = new ItemListingService(client);
    }

    @Test
    void addAndRemoveDelegateToClient() {
        ItemListingJsonDTO item = item();
        when(client.addItem(item)).thenReturn(true);
        when(client.removeItem(9)).thenReturn(false);

        assertTrue(service.addItem(item));
        assertFalse(service.removeItem(9));
        verify(client).addItem(item);
        verify(client).removeItem(9);
    }

    @Test
    void getItemMapsResponseAndMissingItemThrows() {
        ItemListing listing = ItemListing.newBuilder()
                .setId(9)
                .setItemName("Winter Coat")
                .setDescription("A warm coat for winter")
                .setPrice(30)
                .setOwnerId(4)
                .setDate(Date.newBuilder().setYear(2025).setMonth(2).setDay(3))
                .build();
        when(client.getItemById(9)).thenReturn(Optional.of(listing));
        when(client.getItemById(10)).thenReturn(Optional.empty());

        var result = service.getItemById(9);

        assertEquals(9, result.id());
        assertEquals("Winter Coat", result.itemName());
        assertEquals(2025, result.year());
        assertEquals(2, result.month());
        assertEquals(3, result.day());
        assertThrows(NoSuchElementException.class, () -> service.getItemById(10));
    }

    @Test
    void updateRejectsInvalidIdsAndReportsMissingItems() {
        ItemListingJsonDTO item = item();

        assertThrows(InvalidRequestException.class, () -> service.updateItem(0, item));
        verifyNoInteractions(client);

        when(client.updateItem(5, item)).thenReturn(true);
        assertDoesNotThrow(() -> service.updateItem(5, item));

        when(client.updateItem(4, item)).thenReturn(false);
        NoSuchElementException missing = assertThrows(NoSuchElementException.class,
                () -> service.updateItem(4, item));
        assertEquals("Item 4 not found", missing.getMessage());
        verify(client).updateItem(5, item);
        verify(client).updateItem(4, item);
    }

    @Test
    void summariesValidateCriteriaAndMapEmptyDescriptions() {
        ItemSearchCriteriaDTO criteria = new ItemSearchCriteriaDTO(
                "  coat ", List.of(Category.CLOTHING), 1, 2, 5);
        when(client.getItemsSummaries(criteria)).thenReturn(List.of(
                ItemSummary.newBuilder()
                        .setId(3)
                        .setItemName("Coat")
                        .setPrice(20)
                        .setOwnerId(2)
                        .setDate(Date.newBuilder().setYear(2025).setMonth(1).setDay(2))
                        .build()));

        List<ItemSummaryResponseDTO> result = service.getItemSummaries(criteria);

        assertEquals(List.of(new ItemSummaryResponseDTO(3, "Coat", null, 20, 2025, 1, 2, 2)), result);
        verify(client).getItemsSummaries(criteria);
        assertThrows(InvalidRequestException.class,
                () -> service.getItemSummaries(new ItemSearchCriteriaDTO("", List.of(), 0, 0, 0)));
        assertThrows(InvalidRequestException.class,
                () -> service.getItemSummaries(new ItemSearchCriteriaDTO(
                        "x".repeat(ItemListingService.MAX_SEARCH_TITLE_LENGTH + 1), List.of(), 0, 0, 1)));
    }

    @Test
    void thumbnailsDeduplicateIdsAndMapBytes() {
        ItemThumbnail thumbnail = ItemThumbnail.newBuilder()
                .setItemId(3)
                .setFiletype(ImageFileType.PNG)
                .setWidth(2)
                .setHeight(1)
                .setData(ByteString.copyFrom(new byte[]{1, 2}))
                .build();
        when(client.getThumbnails(List.of(3, 5))).thenReturn(List.of(thumbnail));

        List<ThumbnailResponseDTO> result = service.getThumbnails(List.of(3, 3, 5));

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).itemId());
        assertEquals(ImageFileType.PNG, result.get(0).filetype());
        assertEquals(2, result.get(0).width());
        assertEquals(1, result.get(0).height());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[]{1, 2}, result.get(0).data());
        verify(client).getThumbnails(List.of(3, 5));
    }

    @Test
    void thumbnailsRejectEmptyNullNonPositiveAndOversizedIdLists() {
        assertThrows(InvalidRequestException.class, () -> service.getThumbnails(null));
        assertThrows(InvalidRequestException.class, () -> service.getThumbnails(List.of()));
        assertThrows(InvalidRequestException.class, () -> service.getThumbnails(List.of(0)));
        assertThrows(InvalidRequestException.class, () -> service.getThumbnails(
                java.util.Arrays.asList(1, null)));
        assertThrows(InvalidRequestException.class, () -> service.getThumbnails(
                java.util.stream.IntStream.rangeClosed(1, ItemListingService.MAX_THUMBNAIL_IDS + 1)
                        .boxed().toList()));
        verifyNoInteractions(client);
    }

    private static ItemListingJsonDTO item() {
        return new ItemListingJsonDTO("Winter Coat", "A warm coat for winter", 30,
                List.of(Category.CLOTHING),
                List.of(new ImageJsonDTO("coat.png", ImageFileType.PNG, 2, 1, new byte[]{1})), 4);
    }
}