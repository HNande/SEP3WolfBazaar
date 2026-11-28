package dk.via.sep3.mappers;

import com.google.protobuf.ByteString;
import dk.via.sep3.dto.ImageDataDTO;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.dto.ThumbnailDataDTO;
import dk.via.sep3.entities.Category;
import dk.via.sep3.entities.ImageFileType;
import dk.via.sep3.entities.ItemListing;
import dk.via.sep3.entities.Member;
import dk.via.sep3.entities.ItemPicture;
import dk.via.sep3.support.TestImages;
import dk.via.sep3.util.ListingCategories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemListingDataMapperTest {

    @TempDir
    Path tempDirectory;

    @Test
    void mapsAddAndUpdateRequestsToDomainDtos() {
        byte[] imageBytes = TestImages.png(2, 3);
        var image = dk.via.sep3.generated.Image.newBuilder()
                .setName("coat.png")
                .setFiletype(dk.via.sep3.generated.ImageFileType.PNG)
                .setWidth(2)
                .setHeight(3)
                .setData(ByteString.copyFrom(imageBytes))
                .build();
        var addRequest = dk.via.sep3.generated.AddItemListingRequest.newBuilder()
                .setItemName("Coat")
                .setDescription("Warm")
                .setPrice(20)
                .addImages(image)
                .setOwnerId(4)
                .addCategories(dk.via.sep3.generated.Category.CLOTHING)
                .build();

        var addDto = ItemListingDataMapper.toDomainDTO(addRequest);
        assertEquals("Coat", addDto.itemName());
        assertEquals("Warm", addDto.description());
        assertEquals(20, addDto.price());
        assertEquals(4, addDto.ownerId());
        assertEquals(List.of("CLOTHING"), addDto.categoryNames());
        assertEquals("coat.png", addDto.images().get(0).name());
        assertEquals("PNG", addDto.images().get(0).filetype());
        assertArrayEquals(imageBytes, addDto.images().get(0).data());

        var updateRequest = dk.via.sep3.generated.UpdateItemListingRequest.newBuilder()
                .setItemId(9)
                .setItemName("Updated coat")
                .setDescription("Updated")
                .setPrice(25)
                .addImages(image)
                .addCategories(dk.via.sep3.generated.Category.CLOTHING)
                .build();
        var updateDto = ItemListingDataMapper.toUpdateDTO(updateRequest);
        assertEquals(9, updateDto.itemId());
        assertEquals("Updated coat", updateDto.itemName());
        assertEquals(25, updateDto.price());
        assertEquals(List.of("CLOTHING"), updateDto.categoryNames());
        assertArrayEquals(imageBytes, updateDto.images().get(0).data());
    }

    @Test
    void mapsEntityResponseAndReadsImageBytesAndDimensions() throws Exception {
        byte[] imageBytes = TestImages.png(3, 5);
        Path imagePath = tempDirectory.resolve("coat.png");
        Files.write(imagePath, imageBytes);
        ItemListing listing = new ItemListing("Coat", "Warm", 20, member(4));
        listing.setItemId(9);
        listing.setListDate(LocalDate.of(2025, 3, 4));
        listing.getCategories().add(new Category("CLOTHING"));
        listing.getCategories().add(new Category("INVALID"));
        listing.addPicture(new ItemPicture("coat.png", ImageFileType.PNG, imagePath.toString()));

        ItemListingResponseDTO result = ItemListingDataMapper.toResponseDTO(listing);

        assertEquals(9, result.id());
        assertEquals("Coat", result.itemName());
        assertEquals(List.of(dk.via.sep3.generated.Category.CLOTHING), result.categoryList());
        assertEquals(2025, result.year());
        assertEquals(3, result.month());
        assertEquals(4, result.day());
        assertEquals(3, result.imageList().get(0).width());
        assertEquals(5, result.imageList().get(0).height());
        assertArrayEquals(imageBytes, result.imageList().get(0).data());
    }

    @Test
    void mapsListingSummaryAndThumbnailToProtobuf() {
        ItemListingResponseDTO response = new ItemListingResponseDTO(
                7, null, null, 30, List.of(new ImageDataDTO("x.jpg", "jpeg", 2, 4, new byte[]{1, 2})),
                List.of(dk.via.sep3.generated.Category.ELECTRONICS), 2024, 6, 8, 3);

        var proto = ItemListingDataMapper.toResponse(response).getItemlisting();
        assertEquals(7, proto.getId());
        assertEquals("", proto.getItemName());
        assertEquals("", proto.getDescription());
        assertEquals(dk.via.sep3.generated.Category.ELECTRONICS, proto.getCategories(0));
        assertEquals(2024, proto.getDate().getYear());
        assertEquals("x.jpg", proto.getImages(0).getName());
        assertEquals(dk.via.sep3.generated.ImageFileType.JPEG, proto.getImages(0).getFiletype());
        assertArrayEquals(new byte[]{1, 2}, proto.getImages(0).getData().toByteArray());

        var summary = ItemListingDataMapper.toProtoSummary(
                new ItemSummaryDataDTO(7, null, null, 30, LocalDate.of(2024, 6, 8), 3));
        assertEquals("", summary.getItemName());
        assertEquals("", summary.getDescription());
        assertEquals(30, summary.getPrice());
        assertEquals(2024, summary.getDate().getYear());
        assertEquals(6, summary.getDate().getMonth());
        assertEquals(8, summary.getDate().getDay());

        var thumbnail = ItemListingDataMapper.toProtoThumbnail(
                new ThumbnailDataDTO(7, ImageFileType.PNG, 2, 4, new byte[]{3}));
        assertEquals(7, thumbnail.getItemId());
        assertEquals(dk.via.sep3.generated.ImageFileType.PNG, thumbnail.getFiletype());
        assertTrue(thumbnail.getData().equals(ByteString.copyFrom(new byte[]{3})));
    }

    @Test
    void mapsUnknownStoredCategoriesToNoProtoCategory() {
        ItemListing listing = new ItemListing("Coat", "Warm", 20, member(4));
        listing.setListDate(LocalDate.now());
        listing.getCategories().add(new Category("invalid"));

        assertTrue(ItemListingDataMapper.toResponseDTO(listing).categoryList().isEmpty());
        assertEquals(dk.via.sep3.generated.Category.UNSPECIFIED,
                ListingCategories.fromStoredName("invalid"));
    }

    private static Member member(int id) {
        Member member = new Member("First", "Last", "user" + id, "pw", "user" + id + "@example.com");
        member.setMemberId(id);
        return member;
    }
}