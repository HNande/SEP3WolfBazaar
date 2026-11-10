package dk.via.sep3.dao;

import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.entities.ImageFileType;
import dk.via.sep3.entities.ItemListing;
import dk.via.sep3.entities.ItemPicture;
import dk.via.sep3.repository.ItemListingRepository;
import dk.via.sep3.repository.ItemPictureRepository;
import dk.via.sep3.util.ImageResizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class ItemSummaryDAO {

    private static final Logger LOG = LoggerFactory.getLogger(ItemSummaryDAO.class);
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final ItemListingRepository itemListingRepository;
    private final ItemPictureRepository itemPictureRepository;
    private final Path imageDirectory;

    public ItemSummaryDAO(
            ItemListingRepository itemListingRepository,
            ItemPictureRepository itemPictureRepository,
            @Value("${wolfbazaar.images.dir:uploads/images}") String imageDirectory) {
        this.itemListingRepository = itemListingRepository;
        this.itemPictureRepository = itemPictureRepository;
        this.imageDirectory = Path.of(imageDirectory).toAbsolutePath();
    }

    @Transactional(readOnly = true)
    public ItemSummaryPage getItemSummaries(int pageSize, String pageToken) {
        int size = pageSize > 0 ? Math.min(pageSize, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        int afterId = parseToken(pageToken);

        List<ItemListing> rows = itemListingRepository
                .findByItemIdGreaterThanOrderByItemIdAsc(afterId, PageRequest.of(0, size + 1));

        boolean hasMore = rows.size() > size;
        List<ItemListing> pageRows = hasMore ? rows.subList(0, size) : rows;
        String nextToken = hasMore ? String.valueOf(pageRows.get(pageRows.size() - 1).getItemId()) : "";

        List<ItemSummaryDataDTO> summaries = pageRows.stream()
                .map(item -> new ItemSummaryDataDTO(
                        item.getItemId(),
                        item.getItemName(),
                        item.getDescription(),
                        item.getPrice(),
                        item.getListDate()))
                .toList();

        return new ItemSummaryPage(summaries, nextToken);
    }

    @Transactional
    public List<ThumbnailDataDTO> getFirstThumbnails(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<ThumbnailDataDTO> thumbnails = new ArrayList<>();
        for (Object[] row : itemPictureRepository.findFirstPicturesByItemIds(ids)) {
            ItemPicture picture = (ItemPicture) row[0];
            int itemId = (Integer) row[1];
            try {
                thumbnails.add(loadOrCreateThumbnail(itemId, picture));
            } catch (IOException | RuntimeException e) {
                LOG.warn("No thumbnail for item {}: {}", itemId, e.toString());
            }
        }
        return thumbnails;
    }

    private ThumbnailDataDTO loadOrCreateThumbnail(int itemId, ItemPicture picture) throws IOException {
        ImageFileType type = picture.getFileType();

        String cached = picture.getThumbnailPath();
        if (cached != null && Files.exists(Path.of(cached))) {
            byte[] bytes = Files.readAllBytes(Path.of(cached));
            int[] size = ImageResizer.dimensions(bytes);
            return new ThumbnailDataDTO(itemId, type, size[0], size[1], bytes);
        }

        byte[] original = Files.readAllBytes(Path.of(picture.getFilePath()));
        ImageResizer.Resized resized = ImageResizer.fitWithin(original, type.name());

        Files.createDirectories(imageDirectory);
        Path target = imageDirectory.resolve(UUID.randomUUID() + "_thumb" + extensionOf(type));
        Files.write(target, resized.bytes());
        picture.setThumbnailPath(target.toString());

        return new ThumbnailDataDTO(itemId, type, resized.width(), resized.height(), resized.bytes());
    }

    public record ItemSummaryPage(List<ItemSummaryDataDTO> items, String nextPageToken) {}

    private static int parseToken(String pageToken) {
        if (pageToken == null || pageToken.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(pageToken);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid page token");
        }
    }

    private static String extensionOf(ImageFileType type) {
        return type == ImageFileType.PNG ? ".png" : ".jpg";
    }
}
