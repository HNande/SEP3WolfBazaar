package dk.via.sep3.client;

import com.google.protobuf.Empty;
import dk.via.sep3.generated.*;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.repository.ItemListingJsonDTO;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemListingGrpcClientTest {

    private ItemListingServiceGrpc.ItemListingServiceBlockingStub stub;
    private ItemListingGrpcClient client;

    @BeforeEach
    void setUp() {
        stub = mock(ItemListingServiceGrpc.ItemListingServiceBlockingStub.class);
        client = new ItemListingGrpcClient(stub);
    }


    @Nested
    @DisplayName("addItem")
    class AddItemTests {

        private ItemListingJsonDTO validDto;

        @BeforeEach
        void setUp() {
            validDto = new ItemListingJsonDTO("Laptop", "A used laptop", 500, Collections.emptyList(), 1);
        }

        @Test
        @DisplayName("adds item with no images")
        void zero_noImages_succeeds() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addItemListing(any(AddItemListingRequest.class)))
                    .thenReturn(Empty.getDefaultInstance());

            boolean result = client.addItem(validDto);

            assertTrue(result);
        }

        @Test
        @DisplayName("adds a single valid item")
        void one_validItem_returnsTrue() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addItemListing(any(AddItemListingRequest.class)))
                    .thenReturn(Empty.getDefaultInstance());

            boolean result = client.addItem(validDto);

            assertTrue(result);
        }

        @Test
        @DisplayName("adds multiple items sequentially")
        void many_sequentialAdds_allSucceed() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addItemListing(any(AddItemListingRequest.class)))
                    .thenReturn(Empty.getDefaultInstance());

            boolean r1 = client.addItem(validDto);
            boolean r2 = client.addItem(
                    new ItemListingJsonDTO("Phone", "A used phone", 300, Collections.emptyList(), 2)
            );
            boolean r3 = client.addItem(
                    new ItemListingJsonDTO("Tablet", "A used tablet", 200, Collections.emptyList(), 3)
            );

            assertTrue(r1);
            assertTrue(r2);
            assertTrue(r3);
        }

        @Test
        @DisplayName("returns false on INVALID_ARGUMENT")
        void exceptional_invalidArgument_returnsFalse() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addItemListing(any(AddItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(
                            Status.INVALID_ARGUMENT.withDescription("Price must be positive")));

            boolean result = client.addItem(validDto);

            assertFalse(result);
        }

        @Test
        @DisplayName("returns false on INTERNAL error")
        void exceptional_internalError_returnsFalse() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addItemListing(any(AddItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            boolean result = client.addItem(validDto);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getItemById")
    class GetItemByIdTests {

        private ItemListing validItem;

        @BeforeEach
        void setUp() {
            validItem = ItemListing.newBuilder()
                    .setId(1)
                    .setItemName("Laptop")
                    .setDescription("A used laptop")
                    .setPrice(500)
                    .setOwnerId(1)
                    .build();
        }

        @Test
        @DisplayName("returns empty for non-existent item id")
        void zero_idZero_returnsEmpty() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemListing(any(GetItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

            var result = client.getItemById(0);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns item when it exists")
        void one_existingItem_returnsItem() {
            ItemListingResponse response = ItemListingResponse.newBuilder()
                    .setItemlisting(validItem)
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemListing(any(GetItemListingRequest.class)))
                    .thenReturn(response);

            var result = client.getItemById(1);

            assertTrue(result.isPresent());
            assertEquals(1, result.get().getId());
            assertEquals("Laptop", result.get().getItemName());
        }

        @Test
        @DisplayName("returns different items for multiple IDs")
        void many_sequentialGets_returnDifferentItems() {
            ItemListingResponse r1 = ItemListingResponse.newBuilder()
                    .setItemlisting(ItemListing.newBuilder().setId(1).setItemName("Laptop").build())
                    .build();
            ItemListingResponse r2 = ItemListingResponse.newBuilder()
                    .setItemlisting(ItemListing.newBuilder().setId(2).setItemName("Phone").build())
                    .build();
            ItemListingResponse r3 = ItemListingResponse.newBuilder()
                    .setItemlisting(ItemListing.newBuilder().setId(3).setItemName("Tablet").build())
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemListing(any(GetItemListingRequest.class)))
                    .thenReturn(r1, r2, r3);

            var result1 = client.getItemById(1);
            var result2 = client.getItemById(2);
            var result3 = client.getItemById(3);

            assertTrue(result1.isPresent());
            assertTrue(result2.isPresent());
            assertTrue(result3.isPresent());
            assertEquals("Laptop", result1.get().getItemName());
            assertEquals("Phone", result2.get().getItemName());
            assertEquals("Tablet", result3.get().getItemName());
        }

        @Test
        @DisplayName("returns empty when item not found")
        void boundary_notFound_returnsEmpty() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemListing(any(GetItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

            var result = client.getItemById(999);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws InvalidRequestException on INVALID_ARGUMENT")
        void exceptional_invalidArgument_throwsInvalidRequestException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemListing(any(GetItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(
                            Status.INVALID_ARGUMENT.withDescription("Invalid item ID")));

            assertThrows(
                    InvalidRequestException.class,
                    () -> client.getItemById(-1)
            );
        }

        @Test
        @DisplayName("throws UpstreamException on INTERNAL error")
        void exceptional_internal_throwsUpstreamException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemListing(any(GetItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            assertThrows(
                    UpstreamException.class,
                    () -> client.getItemById(1)
            );
        }
    }

    @Nested
    @DisplayName("getItems")
    class GetItemsTests {

        @Test
        @DisplayName("uses default page size when zero is passed")
        void zero_pageSizeZero_usesDefault() {
            ListItemSummariesResponse response = ListItemSummariesResponse.newBuilder().build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenReturn(response);

            var result = client.getItems(0, null);

            assertNotNull(result);
            verify(stub).getListItemSummaries(argThat(req -> req.getPageSize() > 0));
        }

        @Test
        @DisplayName("returns a page with one item")
        void one_singleItem_returnsPage() {
            ListItemSummariesResponse response = ListItemSummariesResponse.newBuilder()
                    .addItems(ItemSummary.newBuilder().setId(1).setItemName("Laptop").setPrice(500).build())
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenReturn(response);

            var result = client.getItems(10, null);

            assertNotNull(result);
            assertEquals(1, result.items().size());
            assertEquals("Laptop", result.items().get(0).getItemName());
        }

        @Test
        @DisplayName("returns a page with multiple items")
        void many_multipleItems_returnsAll() {
            ListItemSummariesResponse response = ListItemSummariesResponse.newBuilder()
                    .addItems(ItemSummary.newBuilder().setId(1).setItemName("Laptop").build())
                    .addItems(ItemSummary.newBuilder().setId(2).setItemName("Phone").build())
                    .addItems(ItemSummary.newBuilder().setId(3).setItemName("Tablet").build())
                    .setNextPageToken("next-page-token")
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenReturn(response);

            var result = client.getItems(10, null);

            assertNotNull(result);
            assertEquals(3, result.items().size());
            assertEquals("next-page-token", result.nextPageToken());
        }

        @Test
        @DisplayName("treats null page token as empty string")
        void boundary_nullPageToken_sentAsEmpty() {
            ListItemSummariesResponse response = ListItemSummariesResponse.newBuilder().build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenReturn(response);

            client.getItems(10, null);

            verify(stub).getListItemSummaries(argThat(req -> req.getPageToken().isEmpty()));
        }

        @Test
        @DisplayName("throws UpstreamException on INTERNAL error")
        void exceptional_internal_throwsUpstreamException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            assertThrows(
                    UpstreamException.class,
                    () -> client.getItems(10, null)
            );
        }
    }

    @Nested
    @DisplayName("getAllItemSummaries")
    class GetAllItemSummariesTests {

        @Test
        @DisplayName("returns empty list when no items exist")
        void zero_noItems_returnsEmptyList() {
            ListItemSummariesResponse response = ListItemSummariesResponse.newBuilder()
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenReturn(response);

            var result = client.getAllItemSummaries();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns one item when only one exists")
        void one_singleItem_returnsOne() {
            ListItemSummariesResponse response = ListItemSummariesResponse.newBuilder()
                    .addItems(ItemSummary.newBuilder().setId(1).setItemName("Laptop").build())
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenReturn(response);

            var result = client.getAllItemSummaries();

            assertEquals(1, result.size());
            assertEquals("Laptop", result.get(0).getItemName());
        }

        @Test
        @DisplayName("collects items across multiple pages")
        void many_paginatedResults_collectedAll() {
            ListItemSummariesResponse page1 = ListItemSummariesResponse.newBuilder()
                    .addItems(ItemSummary.newBuilder().setId(1).setItemName("Laptop").build())
                    .addItems(ItemSummary.newBuilder().setId(2).setItemName("Phone").build())
                    .setNextPageToken("page-2")
                    .build();

            ListItemSummariesResponse page2 = ListItemSummariesResponse.newBuilder()
                    .addItems(ItemSummary.newBuilder().setId(3).setItemName("Tablet").build())
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenReturn(page1, page2);

            var result = client.getAllItemSummaries();

            assertEquals(3, result.size());
            assertEquals("Laptop", result.get(0).getItemName());
            assertEquals("Phone", result.get(1).getItemName());
            assertEquals("Tablet", result.get(2).getItemName());
        }

        @Test
        @DisplayName("throws UpstreamException if pagination fails")
        void exceptional_errorDuringPagination_throwsUpstreamException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getListItemSummaries(any(ListItemListingSummaryRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            assertThrows(
                    UpstreamException.class,
                    () -> client.getAllItemSummaries()
            );
        }
    }


    @Nested
    @DisplayName("getThumbnails")
    class GetThumbnailsTests {

        @Test
        @DisplayName("returns empty list for no IDs")
        void zero_emptyIdList_returnsEmpty() {
            GetItemThumbnailsResponse response = GetItemThumbnailsResponse.newBuilder().build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemThumbnails(any(ListItemThumbnailRequest.class)))
                    .thenReturn(response);

            var result = client.getThumbnails(Collections.emptyList());

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns one thumbnail for one ID")
        void one_singleId_returnsOneThumbnail() {
            GetItemThumbnailsResponse response = GetItemThumbnailsResponse.newBuilder()
                    .addThumbnails(ItemThumbnail.newBuilder()
                            .setItemId(1)
                            .setFiletype(ImageFileType.IMAGE_FILE_TYPE_PNG)
                            .setWidth(100)
                            .setHeight(100)
                            .build())
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemThumbnails(any(ListItemThumbnailRequest.class)))
                    .thenReturn(response);

            var result = client.getThumbnails(List.of(1));

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItemId());
        }

        @Test
        @DisplayName("returns thumbnails for multiple IDs")
        void many_multipleIds_returnMultipleThumbnails() {
            GetItemThumbnailsResponse response = GetItemThumbnailsResponse.newBuilder()
                    .addThumbnails(ItemThumbnail.newBuilder().setItemId(1).setFiletype(ImageFileType.IMAGE_FILE_TYPE_PNG).build())
                    .addThumbnails(ItemThumbnail.newBuilder().setItemId(2).setFiletype(ImageFileType.IMAGE_FILE_TYPE_JPG).build())
                    .addThumbnails(ItemThumbnail.newBuilder().setItemId(3).setFiletype(ImageFileType.IMAGE_FILE_TYPE_JPEG).build())
                    .build();

            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemThumbnails(any(ListItemThumbnailRequest.class)))
                    .thenReturn(response);

            var result = client.getThumbnails(List.of(1, 2, 3));

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("throws UpstreamException on error")
        void exceptional_error_throwsUpstreamException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.getItemThumbnails(any(ListItemThumbnailRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            assertThrows(
                    UpstreamException.class,
                    () -> client.getThumbnails(List.of(1))
            );
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItemTests {

        @Test
        @DisplayName("removes item with id 0")
        void zero_idZero_succeeds() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.removeItemListing(any(RemoveItemListingRequest.class)))
                    .thenReturn(Empty.getDefaultInstance());

            boolean result = client.removeItem(0);

            assertTrue(result);
        }

        @Test
        @DisplayName("removes an existing item")
        void one_existingItem_returnsTrue() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.removeItemListing(any(RemoveItemListingRequest.class)))
                    .thenReturn(Empty.getDefaultInstance());

            boolean result = client.removeItem(1);

            assertTrue(result);
        }

        @Test
        @DisplayName("removes multiple items sequentially")
        void many_sequentialRemoves_allSucceed() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.removeItemListing(any(RemoveItemListingRequest.class)))
                    .thenReturn(Empty.getDefaultInstance());

            boolean r1 = client.removeItem(1);
            boolean r2 = client.removeItem(2);
            boolean r3 = client.removeItem(3);

            assertTrue(r1);
            assertTrue(r2);
            assertTrue(r3);
        }

        @Test
        @DisplayName("returns false when item not found")
        void boundary_notFound_returnsFalse() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.removeItemListing(any(RemoveItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

            boolean result = client.removeItem(999);

            assertFalse(result);
        }

        @Test
        @DisplayName("throws InvalidRequestException on INVALID_ARGUMENT")
        void exceptional_invalidArgument_throwsInvalidRequestException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.removeItemListing(any(RemoveItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(
                            Status.INVALID_ARGUMENT.withDescription("Invalid item ID")));

            assertThrows(
                    InvalidRequestException.class,
                    () -> client.removeItem(-1)
            );
        }

        @Test
        @DisplayName("throws UpstreamException on INTERNAL error")
        void exceptional_internal_throwsUpstreamException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.removeItemListing(any(RemoveItemListingRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            assertThrows(
                    UpstreamException.class,
                    () -> client.removeItem(1)
            );
        }
    }
}
