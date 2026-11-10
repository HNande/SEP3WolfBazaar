package org.example.application.repository;

import com.google.protobuf.ByteString;
import generated.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.emptyToNull;

public final class ItemListingMapper {
  private static final Map<ImageFileTypeJsonDTO, ImageFileType> FILETYPE_TO_PROTO = new EnumMap<>(Map.of(
      ImageFileTypeJsonDTO.PNG, ImageFileType.IMAGE_FILE_TYPE_PNG,
      ImageFileTypeJsonDTO.JPEG, ImageFileType.IMAGE_FILE_TYPE_JPEG,
      ImageFileTypeJsonDTO.JPG, ImageFileType.IMAGE_FILE_TYPE_JPG));

  private static final Map<ImageFileType, ImageFileTypeJsonDTO> FILETYPE_FROM_PROTO =
      FILETYPE_TO_PROTO.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  public static AddItemListingRequest toRequest(ItemListingJsonDTO dto) {
    AddItemListingRequest.Builder builder = AddItemListingRequest.newBuilder()
        .setItemName(dto.itemName())
        .setDescription(dto.description())
        .setPrice(dto.price());

    for (int i = 0; i < dto.imageList().size(); i++) {
    builder.addImages(toImage(dto.imageList().get(i)));
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
        item.getDate().getDay());
  }
  public static ItemSummaryResponseDTO toSummaryResponse(ItemSummary item) {
    return new ItemSummaryResponseDTO(
        item.getId(),
        item.getItemName(),
        emptyToNull(item.getDescription()),
        item.getPrice(),
        item.getDate().getYear(),
        item.getDate().getMonth(),
        item.getDate().getDay());
  }

  public static ThumbnailResponseDTO toThumbnailResponse(ItemThumbnail t) {
    return new ThumbnailResponseDTO(
        t.getItemId(),
        FILETYPE_FROM_PROTO.get(t.getFiletype()),
        t.getWidth(),
        t.getHeight(),
        t.getData().toByteArray());
  }

  private static ImageResponseDTO toImageResponse(Image i) {
    return new ImageResponseDTO(
        i.getName(),
        FILETYPE_FROM_PROTO.get(i.getFiletype()),
        i.getWidth(),
        i.getHeight(),
        i.getData().toByteArray());
  }

  private static Image toImage(ImageJsonDTO dto) {
    return Image.newBuilder()
        .setName(dto.name())
        .setFiletype(FILETYPE_TO_PROTO.get(dto.filetype()))
        .setWidth(dto.width())
        .setHeight(dto.height())
        .setData(ByteString.copyFrom(dto.data()))
        .build();
  }
}
