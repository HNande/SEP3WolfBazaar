package dk.via.sep3.server;

import com.google.protobuf.ByteString;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.entities.ImageFileType;
import dk.via.sep3.service.DataItemListingService;
import dk.via.sep3.support.RecordingStreamObserver;
import dk.via.sep3.support.TestImages;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GrpcItemListingServerTest {

    private DataItemListingService service;
    private GrpcItemListingServer server;

    @BeforeEach
    void setUp() {
        service = mock(DataItemListingService.class);
        server = new GrpcItemListingServer(service);
    }

    @Test
    void addListingMapsRequestBeforeDelegating() {
        when(service.addItem(org.mockito.ArgumentMatchers.any())).thenReturn(response(7));
        byte[] imageBytes = TestImages.png(2, 3);
        var image = dk.via.sep3.generated.Image.newBuilder()
                .setName("coat.png").setFiletype(dk.via.sep3.generated.ImageFileType.PNG)
                .setWidth(2).setHeight(3).setData(ByteString.copyFrom(imageBytes)).build();
        var request = dk.via.sep3.generated.AddItemListingRequest.newBuilder()
                .setItemName("Coat").setDescription("Warm").setPrice(20).setOwnerId(2)
                .addCategories(dk.via.sep3.generated.Category.CLOTHING).addImages(image).build();
        RecordingStreamObserver<com.google.protobuf.Empty> observer = new RecordingStreamObserver<>();

        server.addItemListing(request, observer);

        assertTrue(observer.completed());
        assertEquals(1, observer.values().size());
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(dk.via.sep3.dto.ItemListingDataDTO.class);
        verify(service).addItem(requestCaptor.capture());
        assertEquals("Coat", requestCaptor.getValue().itemName());
        assertEquals(2, requestCaptor.getValue().images().get(0).width());
        assertEquals(List.of("CLOTHING"), requestCaptor.getValue().categoryNames());
    }

    @Test
    void getListingReturnsMappedResponseOrNotFound() {
        when(service.getItemById(7)).thenReturn(Optional.of(response(7)));
        RecordingStreamObserver<dk.via.sep3.generated.ItemListingResponse> found = new RecordingStreamObserver<>();
        server.getItemListing(dk.via.sep3.generated.GetItemListingRequest.newBuilder().setId(7).build(), found);
        assertEquals(7, found.values().get(0).getItemlisting().getId());
        assertTrue(found.completed());

        when(service.getItemById(8)).thenReturn(Optional.empty());
        RecordingStreamObserver<dk.via.sep3.generated.ItemListingResponse> missing = new RecordingStreamObserver<>();
        server.getItemListing(dk.via.sep3.generated.GetItemListingRequest.newBuilder().setId(8).build(), missing);
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(missing.error()).getCode());
        assertFalse(missing.completed());
    }

    @Test
    void catalogueSearchNormalizesTitleMapsCategoriesAndCreatesSortPage() {
        when(service.searchItemSummaries("coat", List.of("GARDENING", "OUTDOOR_GOODS"),
                PageRequest.of(1, 50, Sort.by(Sort.Order.desc("price"), Sort.Order.asc("listDate"),
                        Sort.Order.asc("itemId")))))
                .thenReturn(Optional.of(List.of(new ItemSummaryDataDTO(
                        7, "Coat", "Warm", 20, LocalDate.of(2025, 1, 2), 3))));
        var request = dk.via.sep3.generated.ListItemListingSummaryRequest.newBuilder()
                .setTitle(" coat ").setCounter(2)
                .addCategoryList(dk.via.sep3.generated.Category.GARDENING)
                .setFilter(dk.via.sep3.generated.Filter.newBuilder().setPrice(2).setDate(2)).build();
        RecordingStreamObserver<dk.via.sep3.generated.ListItemSummariesResponse> observer =
                new RecordingStreamObserver<>();

        server.getListItemSummaries(request, observer);

        assertEquals(7, observer.values().get(0).getItems(0).getId());
        assertEquals(2025, observer.values().get(0).getItems(0).getDate().getYear());
        assertEquals(1, observer.values().get(0).getItems(0).getDate().getMonth());
        assertEquals(2, observer.values().get(0).getItems(0).getDate().getDay());
        assertTrue(observer.completed());
        verify(service).searchItemSummaries("coat", List.of("GARDENING", "OUTDOOR_GOODS"),
                PageRequest.of(1, 50, Sort.by(Sort.Order.desc("price"), Sort.Order.asc("listDate"),
                        Sort.Order.asc("itemId"))));
    }

    @Test
    void catalogueSearchRejectsInvalidRequestAndReportsMissingConfiguration() {
        RecordingStreamObserver<dk.via.sep3.generated.ListItemSummariesResponse> invalid =
                new RecordingStreamObserver<>();
        server.getListItemSummaries(dk.via.sep3.generated.ListItemListingSummaryRequest.newBuilder()
                .setCounter(0).build(), invalid);
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalid.error()).getCode());
        verifyNoInteractions(service);

        when(service.searchItemSummaries("", List.of(), PageRequest.of(0, 50,
                Sort.by(Sort.Order.asc("itemId"))))).thenReturn(Optional.empty());
        RecordingStreamObserver<dk.via.sep3.generated.ListItemSummariesResponse> notConfigured =
                new RecordingStreamObserver<>();
        server.getListItemSummaries(dk.via.sep3.generated.ListItemListingSummaryRequest.newBuilder()
                .setCounter(1).build(), notConfigured);
        assertEquals(Status.Code.UNIMPLEMENTED, Status.fromThrowable(notConfigured.error()).getCode());
    }

    @Test
    void invalidSortAndTooLongTitleReturnInvalidArgument() {
        RecordingStreamObserver<dk.via.sep3.generated.ListItemSummariesResponse> invalidSort =
                new RecordingStreamObserver<>();
        server.getListItemSummaries(dk.via.sep3.generated.ListItemListingSummaryRequest.newBuilder()
                .setCounter(1).setFilter(dk.via.sep3.generated.Filter.newBuilder().setDate(9)).build(), invalidSort);
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalidSort.error()).getCode());

        RecordingStreamObserver<dk.via.sep3.generated.ListItemSummariesResponse> longTitle =
                new RecordingStreamObserver<>();
        server.getListItemSummaries(dk.via.sep3.generated.ListItemListingSummaryRequest.newBuilder()
                .setCounter(1).setTitle("x".repeat(101)).build(), longTitle);
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(longTitle.error()).getCode());
        verifyNoInteractions(service);
    }

    @Test
    void thumbnailsAndRemoveMapResultsAndStatus() {
        when(service.getFirstThumbnails(List.of(7))).thenReturn(List.of(
                new ThumbnailDataDTO(7, ImageFileType.PNG, 2, 3, new byte[]{1, 2})));
        RecordingStreamObserver<dk.via.sep3.generated.GetItemThumbnailsResponse> thumbnails =
                new RecordingStreamObserver<>();
        server.getItemThumbnails(dk.via.sep3.generated.ListItemThumbnailRequest.newBuilder()
                .addItemIds(7).build(), thumbnails);
        assertEquals(ByteString.copyFrom(new byte[]{1, 2}), thumbnails.values().get(0).getThumbnails(0).getData());
        assertTrue(thumbnails.completed());

        when(service.removeItem(7)).thenReturn(true);
        RecordingStreamObserver<com.google.protobuf.Empty> removed = new RecordingStreamObserver<>();
        server.removeItemListing(dk.via.sep3.generated.RemoveItemListingRequest.newBuilder()
                .setItemId(7).build(), removed);
        assertTrue(removed.completed());

        when(service.removeItem(8)).thenReturn(false);
        RecordingStreamObserver<com.google.protobuf.Empty> missing = new RecordingStreamObserver<>();
        server.removeItemListing(dk.via.sep3.generated.RemoveItemListingRequest.newBuilder()
                .setItemId(8).build(), missing);
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(missing.error()).getCode());
    }

    private static ItemListingResponseDTO response(int id) {
        return new ItemListingResponseDTO(id, "Coat", "Warm", 20, List.of(),
                List.of(dk.via.sep3.generated.Category.CLOTHING), 2025, 1, 2, 3);
    }
}