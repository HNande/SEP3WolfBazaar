package dk.via.sep3.mappers;

import com.google.protobuf.ByteString;
import dk.via.sep3.dto.*;
import dk.via.sep3.entities.ItemListing;
import dk.via.sep3.generated.*;
import dk.via.sep3.entities.ItemPicture;
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

  public static ItemListingResponseDTO toResponseDTO(ItemListing itemListing) {
    LocalDate date = itemListing.getListDate();
    return new ItemListingResponseDTO(
            itemListing.getItemId(),
            itemListing.getItemName(),
            itemListing.getDescription(),
            itemListing.getPrice(),
            toDTO(itemListing.getPictures()),
            //TODO We are missing category get method
            itemListing.getCategories(),
            date.getYear(),
            date.getMonthValue(),
            date.getDayOfMonth());
  }

  public static List<ImageDataDTO> toDTO(List<ItemPicture> pictures) {
    if (pictures == null) {
      return List.of();
    }
    return pictures.stream()
        .map(ItemListingDataMapper::toImageDTO)
        .toList();
  }
  private static ImageDataDTO toImageDTO(ItemPicture picture) {
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

  public static dk.via.sep3.generated.ItemListing toProtoListing(ItemListingResponseDTO dto) {
    dk.via.sep3.generated.ItemListing.Builder builder = dk.via.sep3.generated.ItemListing.newBuilder()
        .setId(dto.id())
        .setItemName(orEmpty(dto.itemName()))
        .setDescription(orEmpty(dto.description()))
        .setPrice(dto.price())
        .setDate(dk.via.sep3.generated.Date.newBuilder()
            .setYear(dto.year())
            .setMonth(dto.month())
            .setDay(dto.day())
            .build());

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
    dk.via.sep3.generated.Date protoDate =  dk.via.sep3.generated.Date.newBuilder()
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