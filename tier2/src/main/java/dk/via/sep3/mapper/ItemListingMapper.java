package dk.via.sep3.mapper;

import com.google.protobuf.ByteString;
import dk.via.sep3.dto.*;
import dk.via.sep3.generated.*;

import java.util.*;

import static com.google.common.base.Strings.emptyToNull;

public final class ItemListingMapper {
  public static AddItemListingRequest toRequest(ItemListingJsonDTO dto) {
    AddItemListingRequest.Builder builder = AddItemListingRequest.newBuilder()
        .setItemName(dto.itemName())
        .setDescription(dto.description())
        .setPrice(dto.price())
        .setOwnerId(dto.ownerId())
        .addAllCategories(dto.categories());

    for (int i = 0; i < dto.imageList().size(); i++) {
    builder.addImages(toImage(dto.imageList().get(i)));
    }

    return builder.build();
  }

  public static UpdateItemListingRequest toUpdateRequest(int id, ItemListingJsonDTO dto) {
    UpdateItemListingRequest.Builder builder = UpdateItemListingRequest.newBuilder()
        .setItemId(id)
        .setItemName(dto.itemName())
        .setDescription(dto.description())
        .setPrice(dto.price());

    for (ImageJsonDTO image : dto.imageList()) {
      builder.addImages(toImage(image));
    }
    return builder.build();
  }

  public static ItemListingResponseDTO toResponse(ItemListing item) {
      List<ImageResponseDTO> images = new ArrayList<>();
    for(int i = 0; i < item.getImagesList().size();i++)
      images.add(toImageResponse(item.getImages(i)));
    return new ItemListingResponseDTO(
        item.getId(),
        item.getItemName(),
        emptyToNull(item.getDescription()),
        item.getPrice(),
        images,
        item.getCategoriesList(),
        item.getDate().getYear(),
        item.getDate().getMonth(),
        item.getDate().getDay(),
        item.getOwnerId());
  }
  public static ItemSummaryResponseDTO toSummaryResponse(ItemSummary item) {
    return new ItemSummaryResponseDTO(
        item.getId(),
        item.getItemName(),
        emptyToNull(item.getDescription()),
        item.getPrice(),
        item.getOwnerId());

  }

  public static ThumbnailResponseDTO toThumbnailResponse(ItemThumbnail t) {
    return new ThumbnailResponseDTO(
        t.getItemId(),
        t.getFiletype(),
        t.getWidth(),
        t.getHeight(),
        t.getData().toByteArray());
  }

  private static ImageResponseDTO toImageResponse(Image i) {
    return new ImageResponseDTO(
        i.getName(),
        i.getFiletype(),
        i.getWidth(),
        i.getHeight(),
        i.getData().toByteArray());
  }

  private static Image toImage(ImageJsonDTO dto) {
    return Image.newBuilder()
        .setName(dto.name())
        .setFiletype(dto.filetype())
        .setWidth(dto.width())
        .setHeight(dto.height())
        .setData(ByteString.copyFrom(dto.data()))
        .build();
  }
}
