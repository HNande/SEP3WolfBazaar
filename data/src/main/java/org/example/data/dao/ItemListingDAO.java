package org.example.data.dao;

import org.example.data.dto.ImageDataDTO;
import org.example.data.dto.ItemListingDataDTO;
import org.example.data.dto.ItemListingResponseDTO;
import org.example.data.entities.ImageFileType;
import org.example.data.entities.ItemListing;
import org.example.data.entities.ItemPicture;
import org.example.data.mappers.ItemListingDataMapper;
import org.example.data.repository.ItemListingRepository;
import org.example.util.ImageResizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ItemListingDAO {

  private static final int DEFAULT_PAGE_SIZE = 50;
  private static final int MAX_PAGE_SIZE = 200;

  private final ItemListingRepository itemListingRepository;
  private final Path imageDirectory;

  public ItemListingDAO(
          ItemListingRepository itemListingRepository,
          @Value("${wolfbazaar.images.dir:uploads/images}") String imageDirectory) {
    this.itemListingRepository = itemListingRepository;
    this.imageDirectory = Path.of(imageDirectory).toAbsolutePath();
  }

  @Transactional
  public ItemListingResponseDTO addNewItemListing(ItemListingDataDTO dto) {
    ItemListing listing = new ItemListing();
    listing.setItemName(dto.itemName());
    listing.setDescription(dto.description());
    listing.setPrice(dto.price());
    listing.setListDate(LocalDate.now());

    Map<Path, byte[]> filesToWrite = new LinkedHashMap<>();
    if (dto.album() != null && dto.album().images() != null) {
      for (ImageDataDTO image : dto.album().images()) {
        if (image.data() == null || image.data().length == 0) {
          throw new IllegalArgumentException("Image '" + image.name() + "' has no data");
        }
        ImageFileType type = toFileType(image.filetype());
        Path target = imageDirectory.resolve(UUID.randomUUID() + extensionOf(type));

        ImageResizer.Resized thumb;
        try {
          thumb = ImageResizer.fitWithin(image.data(), type.name());
        } catch (IOException e) {
          throw new UncheckedIOException("Could not create thumbnail for '" + image.name() + "'", e);
        }
        Path thumbTarget = imageDirectory.resolve(UUID.randomUUID() + "_thumb" + extensionOf(type));

        listing.addPicture(new ItemPicture(image.name(), type, target.toString(), thumbTarget.toString()));
        filesToWrite.put(target, image.data());
        filesToWrite.put(thumbTarget, thumb.bytes());
      }
    }

    ItemListing saved = itemListingRepository.save(listing);
    writeFiles(filesToWrite);

    return ItemListingDataMapper.toResponseDTO(saved);
  }

  @Transactional
  public boolean removeItemListing(int id)
  {
      Optional<ItemListing> existingOpt = itemListingRepository.findById(id);
      if (existingOpt.isPresent())
      {
          return false;
      }

      ItemListing listing = existingOpt.get();

      List<Path> filesToDelete = new ArrayList<>();
      for (ItemPicture picture : listing.getPictures())
      {
          if(picture.getFilePath() != null && !picture.getThumbnailPath().isEmpty())
          {
              filesToDelete.add(Path.of(picture.getFilePath()));
          }
      }

      itemListingRepository.delete(listing);
      itemListingRepository.flush();

      for(Path path : filesToDelete)
      {
          try
          {
              Files.deleteIfExists(path);
          } catch (IOException ignored) {

          }
      }
      return true;
  }

  @Transactional(readOnly = true)
  public Optional<ItemListingResponseDTO> getItemById(int id) {
    return itemListingRepository.findById(id).map(ItemListingDataMapper::toResponseDTO);
  }

  @Transactional(readOnly = true)
  public ItemListingPage getItems(int pageSize, String pageToken) {
    int size = pageSize > 0 ? Math.min(pageSize, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
    int afterId = parseToken(pageToken);
    List<ItemListing> rows = itemListingRepository
            .findByItemIdGreaterThanOrderByItemIdAsc(afterId, PageRequest.of(0, size + 1));

    boolean hasMore = rows.size() > size;
    List<ItemListing> pageRows = hasMore ? rows.subList(0, size) : rows;
    String nextToken = hasMore ? String.valueOf(pageRows.get(pageRows.size() - 1).getItemId()) : "";

    List<ItemListingResponseDTO> items =
            pageRows.stream().map(ItemListingDataMapper::toResponseDTO).toList();
    return new ItemListingPage(items, nextToken);
  }

  public record ItemListingPage(List<ItemListingResponseDTO> items, String nextPageToken) {}

  private void writeFiles(Map<Path, byte[]> files) {
    List<Path> written = new ArrayList<>();
    try {
      Files.createDirectories(imageDirectory);
      for (Map.Entry<Path, byte[]> entry : files.entrySet()) {
        Files.write(entry.getKey(), entry.getValue());
        written.add(entry.getKey());
      }
    } catch (IOException e) {
      written.forEach(path -> path.toFile().delete());
      throw new UncheckedIOException("Could not store images", e);
    }
  }

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

  private static ImageFileType toFileType(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("Missing image type");
    }
    String upper = raw.toUpperCase(Locale.ROOT);
    if (upper.equals("JPEG")) {
      return ImageFileType.JPG;
    }
    try {
      return ImageFileType.valueOf(upper);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unsupported image type: " + raw);
    }
  }

  private static String extensionOf(ImageFileType type) {
    return switch (type) {
      case PNG -> ".png";
      case JPG, JPEG -> ".jpg";
    };
  }
}
