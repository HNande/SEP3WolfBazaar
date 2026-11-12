package dk.via.sep3.client;

import dk.via.sep3.generated.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.repository.ItemListingJsonDTO;
import dk.via.sep3.repository.ItemListingMapper;
import dk.via.sep3.repository.ItemPageResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class ItemListingGrpcClient  {

    private static final Logger LOG = LoggerFactory.getLogger(ItemListingGrpcClient.class);
    private static final int TIMEOUT_SECONDS = 10;
    private static final int THUMBNAIL_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int SUMMARY_PAGE_SIZE = 200;

    private final ItemListingServiceGrpc.ItemListingServiceBlockingStub stub;

    public ItemListingGrpcClient(ItemListingServiceGrpc.ItemListingServiceBlockingStub stub) {
        this.stub = stub;
    }

    public boolean addItem(ItemListingJsonDTO dto) {
        try {
            stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).addItemListing(ItemListingMapper.toRequest(dto));
            return true;
        } catch (StatusRuntimeException e) {
            translate(e, "add item");
            return false;
        }
    }

    public Optional<ItemListing> getItemById(int id) {
        try {
            GetItemListingRequest request = GetItemListingRequest.newBuilder()
                    .setId(id)
                    .build();

            ItemListing item = stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getItemListing(request)
                    .getItemlisting();

            return Optional.of(item);

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw translate(e, "get item " + id);
        }
    }

    public ItemPageResponseDTO getItems(int pageSize, String pageToken) {
        try {
            ListItemListingSummaryRequest request = ListItemListingSummaryRequest.newBuilder()
                    .setPageSize(pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE)
                    .setPageToken(pageToken == null ? "" : pageToken)
                    .build();

            ListItemSummariesResponse response = stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS).getListItemSummaries(request);
            return new ItemPageResponseDTO(response.getItemsList(), response.getNextPageToken());

        } catch (StatusRuntimeException e) {
            throw translate(e, "list items");
        }
    }

    public List<ItemSummary> getAllItemSummaries() {
        List<ItemSummary> all = new ArrayList<>();
        String token = "";
        try {
            do {
                ListItemListingSummaryRequest request = ListItemListingSummaryRequest.newBuilder()
                        .setPageSize(SUMMARY_PAGE_SIZE)
                        .setPageToken(token)
                        .build();

                ListItemSummariesResponse response = stub
                        .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .getListItemSummaries(request);

                all.addAll(response.getItemsList());
                token = response.getNextPageToken();
            } while (token != null && !token.isEmpty());
        } catch (StatusRuntimeException e) {
            throw translate(e, "list item summaries");
        }
        return all;
    }

    public List<ItemThumbnail> getThumbnails(List<Integer> ids) {
        try {
            ListItemThumbnailRequest request = ListItemThumbnailRequest.newBuilder()
                    .addAllItemIds(ids)
                    .build();

            return stub.withDeadlineAfter(THUMBNAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getItemThumbnails(request)
                    .getThumbnailsList();
        } catch (StatusRuntimeException e) {
            throw translate(e, "get thumbnails");
        }
    }

    public boolean removeItem(int id) {
        try {
            RemoveItemListingRequest request = RemoveItemListingRequest.newBuilder()
                    .setItemId(id)
                    .build();

             stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .removeItemListing(request);
             return true;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return false;
            }
            throw translate(e, "remove item " + id);
        }
    }
    private RuntimeException translate(StatusRuntimeException e, String action) {
        Status status = e.getStatus();
        if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
            String message = status.getDescription() != null && !status.getDescription().isBlank()
                    ? status.getDescription()
                    : "Invalid request";
            LOG.warn("Tier 3 rejected '{}': {}", action, message);
            return new InvalidRequestException(message);
        }
        LOG.error("Could not {}. gRPC status: {}", action, status);
        return new UpstreamException("Could not " + action + ": " + status.getCode(), e);
    }
}
