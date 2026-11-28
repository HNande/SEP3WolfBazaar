package dk.via.sep3.dao;

import dk.via.sep3.dto.ImageDataDTO;
import dk.via.sep3.dto.ItemListingDataDTO;
import dk.via.sep3.dto.UpdateItemListingDataDTO;
import dk.via.sep3.entities.Category;
import dk.via.sep3.entities.ImageFileType;
import dk.via.sep3.entities.ItemListing;
import dk.via.sep3.entities.ItemPicture;
import dk.via.sep3.entities.Member;
import dk.via.sep3.repository.CategoryRepository;
import dk.via.sep3.repository.ItemListingRepository;
import dk.via.sep3.repository.MemberRepository;
import dk.via.sep3.support.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItemListingDAOTest {

    @TempDir
    Path imageDirectory;

    private ItemListingRepository listingRepository;
    private CategoryRepository categoryRepository;
    private MemberRepository memberRepository;
    private ItemListingDAO dao;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ItemListingRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        memberRepository = mock(MemberRepository.class);
        when(memberRepository.findById(anyInt())).thenAnswer(invocation ->
                Optional.of(member(invocation.getArgument(0))));
        dao = new ItemListingDAO(listingRepository, categoryRepository, memberRepository,
                imageDirectory.toString());
    }

    @Test
    void getItemsUsesNextItemIdAndReturnsNextTokenWhenMoreResultsExist() {
        when(listingRepository.findByItemIdGreaterThanOrderByItemIdAsc(5, PageRequest.of(0, 3)))
                .thenReturn(List.of(listing(6), listing(7), listing(8)));

        ItemListingDAO.ItemListingPage page = dao.getItems(2, "5");

        assertEquals(List.of(6, 7), page.items().stream().map(item -> item.id()).toList());
        assertEquals("7", page.nextPageToken());
        verify(listingRepository).findByItemIdGreaterThanOrderByItemIdAsc(5, PageRequest.of(0, 3));
    }

    @Test
    void getItemsUsesDefaultSizeAndReturnsEmptyTokenOnLastPage() {
        when(listingRepository.findByItemIdGreaterThanOrderByItemIdAsc(0, PageRequest.of(0, 51)))
                .thenReturn(List.of(listing(1)));

        ItemListingDAO.ItemListingPage page = dao.getItems(0, null);

        assertEquals(List.of(1), page.items().stream().map(item -> item.id()).toList());
        assertEquals("", page.nextPageToken());
    }

    @Test
    void getItemsRejectsInvalidPageTokenBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class, () -> dao.getItems(10, "next"));
        verifyNoInteractions(listingRepository);
    }

    @Test
    void addNewItemStoresOriginalAndThumbnailAndResolvesCategories() throws Exception {
        byte[] imageBytes = TestImages.png(4, 3);
        ItemListingDataDTO dto = new ItemListingDataDTO("Coat", "Warm", 20,
                List.of(new ImageDataDTO("coat.png", "PNG", 4, 3, imageBytes)),
                2, List.of("CLOTHING"));
        Category category = new Category("CLOTHING");
        when(categoryRepository.findByNameIn(List.of("CLOTHING"))).thenReturn(List.of(category));
        when(listingRepository.save(any(ItemListing.class))).thenAnswer(invocation -> {
            ItemListing saved = invocation.getArgument(0);
            saved.setItemId(12);
            saved.setListDate(LocalDate.of(2025, 1, 2));
            return saved;
        });

        var result = dao.addNewItemListing(dto);

        assertEquals(12, result.id());
        assertEquals(List.of(dk.via.sep3.generated.Category.CLOTHING), result.categoryList());
        assertEquals(1, result.imageList().size());
        assertArrayEquals(imageBytes, result.imageList().get(0).data());
        verify(categoryRepository).findByNameIn(List.of("CLOTHING"));
        verify(memberRepository).findById(2);
        org.mockito.ArgumentCaptor<ItemListing> savedListing =
                org.mockito.ArgumentCaptor.forClass(ItemListing.class);
        verify(listingRepository).save(savedListing.capture());
        assertEquals(2, savedListing.getValue().getOwnerId());
        try (var files = Files.list(imageDirectory)) {
            assertEquals(2, files.count());
        }
    }

    @Test
    void addNewItemRejectsMissingImageDataAndIncompleteCategoryCatalogue() {
        ItemListingDataDTO emptyImage = new ItemListingDataDTO("Coat", "Warm", 20,
                List.of(new ImageDataDTO("coat.png", "PNG", 1, 1, new byte[0])), 2, List.of());
        assertThrows(IllegalArgumentException.class, () -> dao.addNewItemListing(emptyImage));

        ItemListingDataDTO incompleteCategories = new ItemListingDataDTO("Coat", "Warm", 20,
                List.of(new ImageDataDTO("coat.png", "PNG", 1, 1, TestImages.png(1, 1))),
                2, List.of("CLOTHING"));
        when(categoryRepository.findByNameIn(List.of("CLOTHING"))).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> dao.addNewItemListing(incompleteCategories));
        verifyNoInteractions(listingRepository);
    }

    @Test
    void addNewItemRejectsUnknownOwner() {
        when(memberRepository.findById(999)).thenReturn(Optional.empty());
        ItemListingDataDTO dto = new ItemListingDataDTO("Coat", "Warm", 20,
                List.of(), 999, List.of());

        assertThrows(NoSuchElementException.class, () -> dao.addNewItemListing(dto));

        verifyNoInteractions(listingRepository);
    }

    @Test
    void removeItemDeletesListingAndItsImageFiles() throws Exception {
        Path original = Files.write(imageDirectory.resolve("original.png"), new byte[]{1});
        Path thumbnail = Files.write(imageDirectory.resolve("thumbnail.png"), new byte[]{2});
        ItemListing listing = listing(12);
        listing.addPicture(new ItemPicture("original.png", ImageFileType.PNG,
                original.toString(), thumbnail.toString()));
        when(listingRepository.findById(12)).thenReturn(Optional.of(listing));

        assertTrue(dao.removeItemListing(12));

        verify(listingRepository).delete(listing);
        verify(listingRepository).flush();
        assertFalse(Files.exists(original));
        assertFalse(Files.exists(thumbnail));
    }

    @Test
    void removeItemReturnsFalseWhenNotFound() {
        when(listingRepository.findById(99)).thenReturn(Optional.empty());

        assertFalse(dao.removeItemListing(99));
    }

    @Test
    void updateReplacesPicturesAndKeepsBlankFieldsUnchanged() throws Exception {
        Path original = Files.write(imageDirectory.resolve("old.png"), new byte[]{1});
        Path oldThumbnail = Files.write(imageDirectory.resolve("old_thumb.png"), new byte[]{2});
        ItemListing listing = listing(12);
        listing.setItemName("Original name");
        listing.setDescription("Original description");
        listing.addPicture(new ItemPicture("old.png", ImageFileType.PNG,
                original.toString(), oldThumbnail.toString()));
        when(listingRepository.findById(12)).thenReturn(Optional.of(listing));
        byte[] replacement = TestImages.png(2, 2);
        UpdateItemListingDataDTO update = new UpdateItemListingDataDTO(12, " ", null, 30,
                List.of(new ImageDataDTO("new.png", "PNG", 2, 2, replacement)), List.of());

        assertTrue(dao.updateItemListing(update));

        assertEquals("Original name", listing.getItemName());
        assertEquals("Original description", listing.getDescription());
        assertEquals(30, listing.getPrice());
        assertEquals(1, listing.getPictures().size());
        assertEquals("new.png", listing.getPictures().get(0).getFileName());
        assertFalse(Files.exists(original));
        assertFalse(Files.exists(oldThumbnail));
        assertTrue(Files.exists(Path.of(listing.getPictures().get(0).getFilePath())));
        assertTrue(Files.exists(Path.of(listing.getPictures().get(0).getThumbnailPath())));
        verify(listingRepository).saveAndFlush(listing);
    }

    @Test
    void updateReturnsFalseWhenListingDoesNotExistAndRejectsUnreadableImage() {
        when(listingRepository.findById(12)).thenReturn(Optional.empty());
        assertFalse(dao.updateItemListing(new UpdateItemListingDataDTO(12, "Coat", "Warm", 20, null, null)));

        ItemListing listing = listing(12);
        when(listingRepository.findById(12)).thenReturn(Optional.of(listing));
        UpdateItemListingDataDTO invalidImage = new UpdateItemListingDataDTO(12, "Coat", "Warm", 20,
                List.of(new ImageDataDTO("bad.png", "PNG", 1, 1, new byte[]{1, 2, 3})), List.of());
        assertThrows(IllegalArgumentException.class, () -> dao.updateItemListing(invalidImage));
    }

    private static ItemListing listing(int id) {
        ItemListing listing = new ItemListing("Item " + id, "Description", 10, member(2));
        listing.setItemId(id);
        listing.setListDate(LocalDate.of(2025, 1, 2));
        return listing;
    }

    private static Member member(int id) {
        Member member = new Member("First", "Last", "user" + id, "pw", "user" + id + "@example.com");
        member.setMemberId(id);
        return member;
    }
}