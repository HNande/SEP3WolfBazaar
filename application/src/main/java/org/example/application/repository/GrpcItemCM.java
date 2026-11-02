package org.example.application.repository;

import generated.*;
import generated.ItemListingServiceGrpc.ItemListingServiceBlockingStub;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.example.application.exception.InvalidRequestException;
import org.example.application.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class GrpcItemCM implements ItemCM {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcItemCM.class);
    private static final long TIMEOUT_SECONDS = 10;
    private static final long THUMBNAIL_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int SUMMARY_PAGE_SIZE = 200;

    private final ItemListingServiceBlockingStub stub;

    public GrpcItemCM(ItemListingServiceBlockingStub stub) {
        this.stub = stub;
    }

    @Override
    public ItemListing addItem(AddItemListingRequest request) {
        try {
            return stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .addItemListing(request)
                    .getItemlisting();
        } catch (StatusRuntimeException e) {
            throw translate(e, "add item");
        }
    }

    @Override
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

    @Override
    public ItemPage getItems(int pageSize, String pageToken) {
        try {
            ListItemListingsRequest request = ListItemListingsRequest.newBuilder()
                    .setPageSize(pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE)
                    .setPageToken(pageToken == null ? "" : pageToken)
                    .build();

            ListItemListingsResponse response = stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .listItemListings(request);

            return new ItemPage(response.getItemsList(), response.getNextPageToken());

        } catch (StatusRuntimeException e) {
            throw translate(e, "list items");
        }
    }

    @Override
    public List<ItemListing> getAllItems() {
        List<ItemListing> allItems = new ArrayList<>();
        String token = "";
        do {
            ItemPage page = getItems(DEFAULT_PAGE_SIZE, token);
            allItems.addAll(page.items());
            token = page.nextPageToken();
        } while (token != null && !token.isEmpty());

        return allItems;
    }

    @Override
    public List<ItemSummary> getAllItemSummaries() {
        List<ItemSummary> all = new ArrayList<>();
        String token = "";
        try {
            do {
                ListItemListingsRequest request = ListItemListingsRequest.newBuilder()
                        .setPageSize(SUMMARY_PAGE_SIZE)
                        .setPageToken(token)
                        .build();

                ListItemSummariesResponse response = stub
                        .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .listItemSummaries(request);

                all.addAll(response.getItemsList());
                token = response.getNextPageToken();
            } while (token != null && !token.isEmpty());
        } catch (StatusRuntimeException e) {
            throw translate(e, "list item summaries");
        }
        return all;
    }

    @Override
    public List<ItemThumbnail> getThumbnails(List<Integer> ids) {
        try {
            GetItemThumbnailsRequest request = GetItemThumbnailsRequest.newBuilder()
                    .addAllItemIds(ids)
                    .build();

            return stub.withDeadlineAfter(THUMBNAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getItemThumbnails(request)
                    .getThumbnailsList();
        } catch (StatusRuntimeException e) {
            throw translate(e, "get thumbnails");
        }
    }

    public void removeItemListing(RemoveItemListingRequest request){
        try {
        stub.removeItemListing(request);
        }
        catch (StatusRuntimeException e) {
          throw translate(e,"remove item listing");
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
