package org.example.data.server;

import generated.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.example.data.dao.ItemListingDAO;
import org.example.data.dao.ItemSummaryDAO;
import org.example.data.dto.ItemListingResponseDTO;
import org.example.data.dto.ThumbnailDataDTO;
import org.example.data.mappers.ItemListingDataMapper;
import org.example.data.service.DataItemListingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Optional;

@GrpcService
public class GrpcItemListingServer extends ItemListingServiceGrpc.ItemListingServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcItemListingServer.class);
    private final DataItemListingService service;

    public GrpcItemListingServer(DataItemListingService service) {
        this.service = service;
    }

    @Override
    public void addItemListing(AddItemListingRequest request,
                               StreamObserver<ItemListingResponse> responseObserver) {
        try {
            ItemListingResponseDTO created =
                    service.addItem(ItemListingDataMapper.toDomainDTO(request));
            responseObserver.onNext(ItemListingDataMapper.toResponse(created));
            responseObserver.onCompleted();
            LOG.info("Item listing [ id={}, name={} ] added successfully",
                    created.id(), created.itemName());
        } catch (Exception e) {
            fail(responseObserver, e, "add item listing");
        }
    }

    @Override
    public void getItemListing(GetItemListingRequest request,
                               StreamObserver<ItemListingResponse> responseObserver) {
        try {
            Optional<ItemListingResponseDTO> item = service.getItemById(request.getId());
            if (item.isPresent()) {
                responseObserver.onNext(ItemListingDataMapper.toResponse(item.get()));
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Item listing with id " + request.getId() + " not found")
                        .asRuntimeException());
            }
        } catch (Exception e) {
            fail(responseObserver, e, "get item listing");
        }
    }

    @Override
    public void listItemListings(ListItemListingsRequest request,
                                 StreamObserver<ListItemListingsResponse> responseObserver) {
        try {
            ItemListingDAO.ItemListingPage page =
                    service.getItems(request.getPageSize(), request.getPageToken());
            responseObserver.onNext(ListItemListingsResponse.newBuilder()
                    .addAllItems(page.items().stream()
                            .map(ItemListingDataMapper::toProtoListing)
                            .toList())
                    .setNextPageToken(page.nextPageToken())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "list item listings");
        }
    }

    @Override
    public void listItemSummaries(ListItemListingsRequest request,
                                  StreamObserver<ListItemSummariesResponse> responseObserver) {
        try {
            ItemSummaryDAO.ItemSummaryPage page = service.getItemSummaries(request.getPageSize(), request.getPageToken());

            ListItemSummariesResponse response = ListItemSummariesResponse.newBuilder()
                    .addAllItems(page.items().stream()
                            .map(ItemListingDataMapper::toProtoSummary)
                            .toList())
                    .setNextPageToken(page.nextPageToken())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "list item summaries");
        }
    }

    @Override
    public void getItemThumbnails(GetItemThumbnailsRequest request,
                                  StreamObserver<GetItemThumbnailsResponse> responseObserver) {
        try {
            List<Integer> ids = request.getItemIdsList();
            List<ThumbnailDataDTO> thumbnails = service.getFirstThumbnails(ids);

            GetItemThumbnailsResponse response = GetItemThumbnailsResponse.newBuilder()
                    .addAllThumbnails(thumbnails.stream()
                            .map(ItemListingDataMapper::toProtoThumbnail)
                            .toList())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "get item thumbnails");
        }
    }

    @Override
    public void removeItemListing(RemoveItemListingRequest request,
                                  StreamObserver<StatusResponse> responseObserver) {
        try {
            boolean removed = service.removeItem(request.getItemId());
            if (removed) {
                responseObserver.onNext(StatusResponse.newBuilder()
                        .setResponse("Success")
                        .build());
                responseObserver.onCompleted();
                LOG.info("Item listing [ id={} ] removed successfully", request.getItemId());
            } else {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Item listing with id " + request.getItemId() + " not found")
                        .asRuntimeException());
            }
        } catch (Exception e) {
            fail(responseObserver, e, "remove item listing");
        }
    }

    private void fail(StreamObserver<?> responseObserver, Exception e, String action) {
        IllegalArgumentException invalid = findIllegalArgument(e);
        if (invalid != null) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(invalid.getMessage())
                    .asRuntimeException());
        } else {
            LOG.error("Could not {}", action, e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Could not " + action)
                    .asRuntimeException());
        }
    }

    private static IllegalArgumentException findIllegalArgument(Throwable t) {
        for (int depth = 0; t != null && depth < 10; depth++, t = t.getCause()) {
            if (t instanceof IllegalArgumentException iae) {
                return iae;
            }
        }
        return null;
    }
}