package dk.via.sep3.server;

import com.google.protobuf.Empty;
import dk.via.sep3.generated.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import dk.via.sep3.dao.ItemSummaryDAO;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.mappers.ItemListingDataMapper;
import dk.via.sep3.service.DataItemListingService;
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
        StreamObserver<Empty> responseObserver) {
        try {
            ItemListingResponseDTO created =
                service.addItem(ItemListingDataMapper.toDomainDTO(request));
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
        try {
            ItemSummaryDAO.ItemSummaryPage page =
                service.getItemSummaries(request.getPageSize(), request.getPageToken());

            responseObserver.onNext(ListItemSummariesResponse.newBuilder()
                .addAllItems(page.items().stream()
                    .map(ItemListingDataMapper::toProtoSummary)
                    .toList())
                .setNextPageToken(page.nextPageToken())
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "list item summaries");
        }
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