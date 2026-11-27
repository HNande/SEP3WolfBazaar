package dk.via.sep3.server;

import com.google.protobuf.Empty;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.generated.*;
import dk.via.sep3.mappers.ItemListingDataMapper;
import dk.via.sep3.service.DataItemListingService;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GrpcService
public class GrpcItemListingServer extends ItemListingServiceGrpc.ItemListingServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcItemListingServer.class);
    private static final int MAX_SEARCH_TITLE_LENGTH = 100;
    private static final int SEARCH_PAGE_SIZE = 50;
    private final DataItemListingService service;

    public GrpcItemListingServer(DataItemListingService service) {
        this.service = service;
    }

    @Override
    public void addItemListing(AddItemListingRequest request,
        StreamObserver<Empty> responseObserver) {
        try {
            ItemListingResponseDTO created = service.addItem(ItemListingDataMapper.toDomainDTO(request));
            responseObserver.onNext(Empty.getDefaultInstance());
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
    public void getListItemSummaries(ListItemListingSummaryRequest request,
                                     StreamObserver<ListItemSummariesResponse> responseObserver) {
        Context context = Context.current();
        if (context.isCancelled()) {
            return;
        }

        try {
            String title = request.getTitle().strip();
            if (title.length() > MAX_SEARCH_TITLE_LENGTH) {
                throw new IllegalArgumentException(
                        "Title must be at most " + MAX_SEARCH_TITLE_LENGTH + " characters");
            }
            if (request.getCounter() < 1) {
                throw new IllegalArgumentException("Counter must be at least 1");
            }

            List<String> categories = request.getCategoryListList().stream()
                    .map(GrpcItemListingServer::searchCategoryName)
                    .distinct()
                    .toList();
            PageRequest page = PageRequest.of(
                    request.getCounter() - 1, SEARCH_PAGE_SIZE, searchSort(request.getFilter()));
            Optional<List<ItemSummaryDataDTO>> items = service.searchItemSummaries(title, categories, page);
            if (context.isCancelled()) {
                return;
            }
            if (items.isEmpty()) {
                responseObserver.onError(Status.UNIMPLEMENTED
                        .withDescription("Catalogue search query is not configured")
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(ListItemSummariesResponse.newBuilder()
                    .addAllItems(items.get().stream()
                            .map(ItemListingDataMapper::toProtoSummary)
                            .toList())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (!context.isCancelled()) {
                fail(responseObserver, e, "list item summaries");
            }
        }
    }

    private static String searchCategoryName(Category category)
    {
        if(category == Category.UNSPECIFIED || category == category.UNRECOGNIZED)
        {
            throw new IllegalArgumentException("Unknown search category");
        }
        return category.name();
    }

    private static Sort searchSort(Filter filter) {
        List<Sort.Order> orders = new ArrayList<>();
        switch (filter.getPrice()) {
            case 0 -> { }
            case 1 -> orders.add(Sort.Order.asc("price"));
            case 2 -> orders.add(Sort.Order.desc("price"));
            default -> throw new IllegalArgumentException("Price order must be 0, 1, or 2");
        }
        switch (filter.getDate()) {
            case 0 -> { }
            case 1 -> orders.add(Sort.Order.desc("listDate"));
            case 2 -> orders.add(Sort.Order.asc("listDate"));
            default -> throw new IllegalArgumentException("Date order must be 0, 1, or 2");
        }
        orders.add(Sort.Order.asc("itemId"));
        return Sort.by(orders);
    }

    @Override
    public void getItemThumbnails(ListItemThumbnailRequest request,
        StreamObserver<GetItemThumbnailsResponse> responseObserver) {
        try {
            List<Integer> ids = request.getItemIdsList();
            List<ThumbnailDataDTO> thumbnails = service.getFirstThumbnails(ids);

            responseObserver.onNext(GetItemThumbnailsResponse.newBuilder()
                .addAllThumbnails(thumbnails.stream()
                    .map(ItemListingDataMapper::toProtoThumbnail)
                    .toList())
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "get item thumbnails");
        }
    }

    @Override
    public void removeItemListing(RemoveItemListingRequest request,
        StreamObserver<Empty> responseObserver) {
        try {
            boolean removed = service.removeItem(request.getItemId());
            if (removed) {
                responseObserver.onNext(Empty.getDefaultInstance());
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
    //TODO: Add picture and keyword update
    @Override
    public void updateItemListing(UpdateItemListingRequest request,
                                  io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            boolean updated = service.updateItem(request);
            if (updated) {
                responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
                responseObserver.onCompleted();
                LOG.info("Item listing [ id={} ] updated successfully", request.getItemId());
            } else {
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Item listing with id " + request.getItemId() + " not found")
                        .asRuntimeException());
            }
        } catch (Exception e) {
            fail(responseObserver, e, "update item listing");
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