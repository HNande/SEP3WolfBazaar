package dk.via.sep3.dao;

import dk.via.sep3.dto.ImageDataDTO;
import dk.via.sep3.dto.ItemListingDataDTO;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.UpdateItemListingDataDTO;
import dk.via.sep3.entities.ImageFileType;
import dk.via.sep3.entities.ItemListing;
import dk.via.sep3.entities.Member;
import dk.via.sep3.entities.ItemPicture;
import dk.via.sep3.mappers.ItemListingDataMapper;
import dk.via.sep3.repository.ItemListingRepository;
import dk.via.sep3.repository.MemberRepository;
import dk.via.sep3.util.ImageResizer;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import dk.via.sep3.entities.Category;
import dk.via.sep3.repository.CategoryRepository;
import java.util.HashSet;
import java.util.Set;

@Repository
public class ItemListingDAO {

  private static final int DEFAULT_PAGE_SIZE = 50;
  private static final int MAX_PAGE_SIZE = 200;

  private final ItemListingRepository itemListingRepository;
  private final Path imageDirectory;
  private final CategoryRepository categoryRepository;
  private final MemberRepository memberRepository;

  public ItemListingDAO(
          ItemListingRepository itemListingRepository,
          CategoryRepository categoryRepository,
          MemberRepository memberRepository,
          @Value("${wolfbazaar.images.dir:uploads/images}") String imageDirectory) {
    this.itemListingRepository = itemListingRepository;
    this.categoryRepository = categoryRepository;
    this.memberRepository = memberRepository;
    this.imageDirectory = Path.of(imageDirectory).toAbsolutePath();
  }

  @Transactional
  public ItemListingResponseDTO addNewItemListing(ItemListingDataDTO dto) {
    ItemListing listing = new ItemListing();
    listing.setItemName(dto.itemName());
    listing.setDescription(dto.description());
    listing.setPrice(dto.price());
    listing.setListDate(LocalDate.now());
    Member owner = memberRepository.findById(dto.ownerId())
            .orElseThrow(() -> new NoSuchElementException("Member " + dto.ownerId() + " not found"));
    listing.setOwner(owner);
    listing.getCategories().addAll(resolveCategories(dto.categoryNames()));

    Map<Path, byte[]> filesToWrite = new LinkedHashMap<>();
    attachPictures(listing, dto.images(), filesToWrite);

    ItemListing saved = itemListingRepository.save(listing);
    writeFiles(filesToWrite);

    return ItemListingDataMapper.toResponseDTO(saved);
  }

  @Transactional
  public boolean removeItemListing(int id) {
    Optional<ItemListing> existingOpt = itemListingRepository.findById(id);
    if (existingOpt.isEmpty()) {
      return false;
    }

    ItemListing listing = existingOpt.get();
    List<Path> filesToDelete = picturePaths(listing.getPictures());

    itemListingRepository.delete(listing);
    itemListingRepository.flush();
    deleteFiles(filesToDelete);
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

  @Transactional
  public boolean updateItemListing(UpdateItemListingDataDTO dto) {
    Optional<ItemListing> existingOpt = itemListingRepository.findById(dto.itemId());
    if (existingOpt.isEmpty()) {
      return false;
    }

    ItemListing listing = existingOpt.get();

    if (dto.itemName() != null && !dto.itemName().isBlank()) {
      listing.setItemName(dto.itemName());
    }
    if (dto.description() != null && !dto.description().isBlank()) {
      listing.setDescription(dto.description());
    }
    listing.setPrice(dto.price());

    if (dto.categoryNames() != null && !dto.categoryNames().isEmpty()) {
      Set<Category> categories = resolveCategories(dto.categoryNames());
      listing.getCategories().clear();
      listing.getCategories().addAll(categories);
    }

    Map<Path, byte[]> filesToWrite = new LinkedHashMap<>();
    List<Path> filesToDelete = new ArrayList<>();
    if (dto.images() != null && !dto.images().isEmpty()) {
      // The update carries the complete picture set, so it replaces the current pictures.
      List<ItemPicture> oldPictures = new ArrayList<>(listing.getPictures());
      filesToDelete.addAll(picturePaths(oldPictures));
      oldPictures.forEach(listing::removePicture);
      attachPictures(listing, dto.images(), filesToWrite);
    }

    itemListingRepository.saveAndFlush(listing);
    writeFiles(filesToWrite);
    deleteFiles(filesToDelete);
    return true;
  }

  private void attachPictures(ItemListing listing, List<ImageDataDTO> images,
                              Map<Path, byte[]> filesToWrite) {
    if (images == null) {
      return;
    }
    for (ImageDataDTO image : images) {
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

  private static List<Path> picturePaths(List<ItemPicture> pictures) {
    List<Path> paths = new ArrayList<>();
    for (ItemPicture picture : pictures) {
      if (picture.getFilePath() != null && !picture.getFilePath().isBlank()) {
        paths.add(Path.of(picture.getFilePath()));
      }
      if (picture.getThumbnailPath() != null && !picture.getThumbnailPath().isBlank()) {
        paths.add(Path.of(picture.getThumbnailPath()));
      }
    }
    return paths;
  }

  private static void deleteFiles(List<Path> paths) {
    for (Path path : paths) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException ignored) {
        // An orphaned file on disk is not worth failing the request over.
      }
    }
  }

  private Set<Category> resolveCategories(List<String> names) {
    if (names.isEmpty()) {
      return Set.of();
    }

    Set<Category> categories = new HashSet<>(categoryRepository.findByNameIn(names));
    if (categories.size() != names.size()) {
      throw new IllegalStateException("The listing category catalogue is incomplete");
    }
    return categories;
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
