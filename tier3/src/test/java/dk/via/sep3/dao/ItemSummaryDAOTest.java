package dk.via.sep3.dao;

import dk.via.sep3.entities.ImageFileType;
import dk.via.sep3.entities.ItemListing;
import dk.via.sep3.entities.ItemPicture;
import dk.via.sep3.repository.ItemListingRepository;
import dk.via.sep3.repository.ItemPictureRepository;
import dk.via.sep3.support.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemSummaryDAOTest {

    @TempDir
    Path imageDirectory;

    private ItemListingRepository listingRepository;
    private ItemPictureRepository pictureRepository;
    private ItemSummaryDAO dao;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ItemListingRepository.class);
        pictureRepository = mock(ItemPictureRepository.class);
        dao = new ItemSummaryDAO(listingRepository, pictureRepository, imageDirectory.toString());
    }

    @Test
    void getItemSummariesMapsRowsAndBuildsNextToken() {
        when(listingRepository.findByItemIdGreaterThanOrderByItemIdAsc(2, PageRequest.of(0, 3)))
                .thenReturn(List.of(listing(3), listing(4), listing(5)));

        ItemSummaryDAO.ItemSummaryPage page = dao.getItemSummaries(2, "2");

        assertEquals(List.of(3, 4), page.items().stream().map(item -> item.itemId()).toList());
        assertEquals("4", page.nextPageToken());
        assertEquals("Item 3", page.items().get(0).itemName());
        assertEquals(LocalDate.of(2025, 1, 2), page.items().get(0).listDate());
    }

    @Test
    void getItemSummariesRejectsInvalidTokenBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class, () -> dao.getItemSummaries(10, "invalid"));
        verifyNoInteractions(listingRepository);
    }

    @Test
    void getFirstThumbnailsUsesCachedFileAndDimensions() throws Exception {
        byte[] bytes = TestImages.png(12, 7);
        Path cached = Files.write(imageDirectory.resolve("cached.png"), bytes);
        ItemPicture picture = new ItemPicture("original.png", ImageFileType.PNG,
                imageDirectory.resolve("original.png").toString(), cached.toString());
        when(pictureRepository.findFirstPicturesByItemIds(List.of(8)))
                .thenReturn(List.<Object[]>of(new Object[]{picture, 8}));

        var result = dao.getFirstThumbnails(List.of(8));

        assertEquals(1, result.size());
        assertEquals(8, result.get(0).itemId());
        assertEquals(12, result.get(0).width());
        assertEquals(7, result.get(0).height());
        assertArrayEquals(bytes, result.get(0).bytes());
    }

    @Test
    void getFirstThumbnailsCreatesAndCachesMissingThumbnailFromOriginal() throws Exception {
        byte[] originalBytes = TestImages.png(450, 225);
        Path original = Files.write(imageDirectory.resolve("original.png"), originalBytes);
        ItemPicture picture = new ItemPicture("original.png", ImageFileType.PNG,
                original.toString(), null);
        when(pictureRepository.findFirstPicturesByItemIds(List.of(8)))
                .thenReturn(List.<Object[]>of(new Object[]{picture, 8}));

        var result = dao.getFirstThumbnails(List.of(8));

        assertEquals(1, result.size());
        assertEquals(200, result.get(0).width());
        assertEquals(100, result.get(0).height());
        assertEquals(result.get(0).bytes().length, Files.size(Path.of(picture.getThumbnailPath())));
    }

    @Test
    void getFirstThumbnailsSkipsPicturesThatCannotBeRead() {
        ItemPicture picture = new ItemPicture("missing.png", ImageFileType.PNG,
                imageDirectory.resolve("missing.png").toString());
        when(pictureRepository.findFirstPicturesByItemIds(List.of(8)))
                .thenReturn(List.<Object[]>of(new Object[]{picture, 8}));

        assertEquals(List.of(), dao.getFirstThumbnails(List.of(8)));
    }

    @Test
    void emptyThumbnailRequestReturnsEmptyWithoutQuery() {
        assertEquals(List.of(), dao.getFirstThumbnails(List.of()));
        verifyNoInteractions(pictureRepository);
    }

    private ItemListing listing(int id) {
        ItemListing listing = new ItemListing("Item " + id, "Description", id * 10, 3);
        listing.setItemId(id);
        listing.setListDate(LocalDate.of(2025, 1, 2));
        return listing;
    }
}