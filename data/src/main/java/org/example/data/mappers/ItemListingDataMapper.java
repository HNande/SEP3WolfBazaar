package org.example.data.mappers;

import com.google.protobuf.ByteString;
import generated.Date;
import generated.*;
import org.example.data.dto.*;
import org.example.data.entities.ItemPicture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

public class ItemListingDataMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ItemListingDataMapper.class);

  public static ItemListingDataDTO toDomainDTO(AddItemListingRequest request) {
    List<ImageDataDTO> images = request.getImagesList().stream()
        .map(ItemListingDataMapper::toImageDTO)
        .toList();
    return new ItemListingDataDTO(
        request.getItemName(),
        request.getDescription(),
        request.getPrice(),
        new AlbumDataDTO(request.getItemName() + " Album", images));
  }

  public static ItemListingResponseDTO toResponseDTO(
          org.example.data.entities.ItemListing itemListing) {
    LocalDate date = itemListing.getListDate();
    return new ItemListingResponseDTO(
            itemListing.getItemId(),
            itemListing.getItemName(),
            itemListing.getDescription(),
            itemListing.getPrice(),
            toAlbumDTO(itemListing.getItemName(), itemListing.getPictures()),
            date.getYear(),
            date.getMonthValue(),
            date.getDayOfMonth());
  }

  private static AlbumDataDTO toAlbumDTO(String itemName, List<ItemPicture> pictures) {
    List<ImageDataDTO> images = pictures.stream()
            .map(ItemListingDataMapper::toDTO)
            .toList();
    return new AlbumDataDTO(itemName + " Album", images);
  }

  public static ImageDataDTO toDTO(ItemPicture picture) {
    String fileType = picture.getFileType() != null ? picture.getFileType().name() : "PNG";
    try {
      byte[] fileData = Files.readAllBytes(Paths.get(picture.getFilePath()));

      int width = 0;
      int height = 0;
      BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(fileData));
      if (decoded != null) {
        width = decoded.getWidth();
        height = decoded.getHeight();
      }
      return new ImageDataDTO(picture.getFileName(), fileType, width, height, fileData);

    } catch (Exception e) {
      LOG.error("Could not read image file {}", picture.getFilePath(), e);
      return new ImageDataDTO(picture.getFileName(), fileType, 0, 0, new byte[0]);
    }
  }

  public static ItemListingResponse toResponse(ItemListingResponseDTO dto) {
    return ItemListingResponse.newBuilder()
            .setItemlisting(toProtoListing(dto))
            .build();
  }

  public static ItemListing toProtoListing(ItemListingResponseDTO dto) {
    ItemListing.Builder builder = ItemListing.newBuilder()
        .setId(dto.id())
        .setItemName(orEmpty(dto.itemName()))
        .setDescription(orEmpty(dto.description()))
        .setPrice(dto.price())
        .setCreatedAt(Date.newBuilder()
            .setYear(dto.year())
            .setMonth(dto.month())
            .setDay(dto.day())
            .build());

    if (dto.album() != null && dto.album().images() != null) {
      builder.addAllImages(dto.album().images().stream()
          .map(ItemListingDataMapper::toProto)
          .toList());
    }
    return builder.build();
  }

  private static ImageDataDTO toImageDTO(Image image) {
    return new ImageDataDTO(
        image.getName(),
        fromProtoFileType(image.getFiletype()),
        image.getWidth(),
        image.getHeight(),
        image.getData().toByteArray());
  }

  private static Image toProto(ImageDataDTO dto) {
    return Image.newBuilder()
        .setName(orEmpty(dto.name()))
        .setFiletype(toProtoFileType(dto.filetype()))
        .setWidth(dto.width())
        .setHeight(dto.height())
        .setData(ByteString.copyFrom(dto.data()))
        .build();
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  public static ItemSummary toProtoSummary(ItemSummaryDataDTO dto) {
    Date protoDate = Date.newBuilder()
            .setYear(dto.listDate().getYear())
            .setMonth(dto.listDate().getMonthValue())
            .setDay(dto.listDate().getDayOfMonth())
            .build();

    return ItemSummary.newBuilder()
            .setId(dto.itemId())
            .setItemName(orEmpty(dto.itemName()))
            .setDescription(orEmpty(dto.description()))
            .setPrice(dto.price())
            .setDate(protoDate)
            .build();
  }

  public static ItemThumbnail toProtoThumbnail(ThumbnailDataDTO dto) {
    return ItemThumbnail.newBuilder()
        .setItemId(dto.itemId())
        .setFiletype(toProtoFileType(dto.fileType().name()))
        .setWidth(dto.width())
        .setHeight(dto.height())
        .setData(ByteString.copyFrom(dto.bytes()))
        .build();
  }

  private static ImageFileType toProtoFileType(String name) {
    if (name == null) return ImageFileType.IMAGE_FILE_TYPE_PNG;
    return switch (name.toUpperCase()) {
      case "JPG" -> ImageFileType.IMAGE_FILE_TYPE_JPG;
      case "JPEG" -> ImageFileType.IMAGE_FILE_TYPE_JPEG;
      case "PNG" -> ImageFileType.IMAGE_FILE_TYPE_PNG;
      default -> ImageFileType.IMAGE_FILE_TYPE_UNSPECIFIED;
    };
  }

  private static String fromProtoFileType(ImageFileType type) {
    return switch (type) {
      case IMAGE_FILE_TYPE_JPG -> "JPG";
      case IMAGE_FILE_TYPE_JPEG -> "JPEG";
      default -> "PNG";
    };
  }


}