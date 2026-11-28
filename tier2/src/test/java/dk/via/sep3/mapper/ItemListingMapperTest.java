package dk.via.sep3.mapper;

import com.google.protobuf.ByteString;
import dk.via.sep3.dto.ImageJsonDTO;
import dk.via.sep3.dto.ImageResponseDTO;
import dk.via.sep3.dto.ItemListingJsonDTO;
import dk.via.sep3.dto.ItemSummaryResponseDTO;
import dk.via.sep3.dto.ThumbnailResponseDTO;
import dk.via.sep3.generated.Category;
import dk.via.sep3.generated.Date;
import dk.via.sep3.generated.Image;
import dk.via.sep3.generated.ImageFileType;
import dk.via.sep3.generated.ItemListing;
import dk.via.sep3.generated.ItemSummary;
import dk.via.sep3.generated.ItemThumbnail;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemListingMapperTest {

    @Test
    void toRequestMapsListingAndImageFields() {
        ItemListingJsonDTO dto = new ItemListingJsonDTO("Winter Coat", "Warm and waterproof", 25,
                List.of(Category.CLOTHING),
                List.of(new ImageJsonDTO("coat.jpg", ImageFileType.JPG, 10, 8, new byte[]{4, 5})), 7);

        var request = ItemListingMapper.toRequest(dto);

        assertEquals("Winter Coat", request.getItemName());
        assertEquals("Warm and waterproof", request.getDescription());
        assertEquals(25, request.getPrice());
        assertEquals(7, request.getOwnerId());
        assertEquals(List.of(Category.CLOTHING), request.getCategoriesList());
        Image image = request.getImages(0);
        assertEquals("coat.jpg", image.getName());
        assertEquals(ImageFileType.JPG, image.getFiletype());
        assertEquals(10, image.getWidth());
        assertEquals(8, image.getHeight());
        assertArrayEquals(new byte[]{4, 5}, image.getData().toByteArray());
    }

    @Test
    void toUpdateRequestIncludesListingIdAndImages() {
        ItemListingJsonDTO dto = new ItemListingJsonDTO("Winter Coat", "Warm and waterproof", 25,
                List.of(Category.CLOTHING),
                List.of(new ImageJsonDTO("coat.jpg", ImageFileType.JPG, 10, 8, new byte[]{4})), 7);

        var request = ItemListingMapper.toUpdateRequest(12, dto);

        assertEquals(12, request.getItemId());
        assertEquals("Winter Coat", request.getItemName());
        assertEquals(List.of(Category.CLOTHING), request.getCategoriesList());
        assertEquals("coat.jpg", request.getImages(0).getName());
        assertArrayEquals(new byte[]{4}, request.getImages(0).getData().toByteArray());
    }

    @Test
    void toResponseMapsListingDateCategoriesAndImages() {
        ItemListing listing = ItemListing.newBuilder()
                .setId(12)
                .setItemName("Winter Coat")
                .setDescription("Warm and waterproof")
                .setPrice(25)
                .setOwnerId(7)
                .setDate(Date.newBuilder().setYear(2025).setMonth(4).setDay(9))
                .addCategories(Category.CLOTHING)
                .addImages(Image.newBuilder().setName("coat.jpg").setFiletype(ImageFileType.JPG)
                        .setWidth(10).setHeight(8).setData(ByteString.copyFrom(new byte[]{4, 5})))
                .build();

        var response = ItemListingMapper.toResponse(listing);

        assertEquals(12, response.id());
        assertEquals("Winter Coat", response.itemName());
        assertEquals("Warm and waterproof", response.description());
        assertEquals(25, response.price());
        assertEquals(7, response.ownerId());
        assertEquals(List.of(Category.CLOTHING), response.categories());
        assertEquals(2025, response.year());
        assertEquals(4, response.month());
        assertEquals(9, response.day());
        ImageResponseDTO image = response.imageList().get(0);
        assertEquals("coat.jpg", image.name());
        assertEquals(ImageFileType.JPG, image.filetype());
        assertEquals(10, image.width());
        assertEquals(8, image.height());
        assertArrayEquals(new byte[]{4, 5}, image.data());
    }

    @Test
    void summaryAndThumbnailResponsesNormalizeEmptyDescriptionAndCopyData() {
        ItemSummaryResponseDTO summary = ItemListingMapper.toSummaryResponse(ItemSummary.newBuilder()
                .setId(3).setItemName("Table").setDescription("").setPrice(18).setOwnerId(2)
                .setDate(Date.newBuilder().setYear(2025).setMonth(4).setDay(9)).build());
        ThumbnailResponseDTO thumbnail = ItemListingMapper.toThumbnailResponse(ItemThumbnail.newBuilder()
                .setItemId(3).setFiletype(ImageFileType.PNG).setWidth(2).setHeight(1)
                .setData(ByteString.copyFrom(new byte[]{1, 2})).build());

        assertNull(summary.description());
        assertEquals(new ItemSummaryResponseDTO(3, "Table", null, 18, 2025, 4, 9, 2), summary);
        assertEquals(3, thumbnail.itemId());
        assertEquals(ImageFileType.PNG, thumbnail.filetype());
        assertArrayEquals(new byte[]{1, 2}, thumbnail.data());
    }
}