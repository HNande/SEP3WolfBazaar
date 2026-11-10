package dk.via.sep3.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "item_picture")
public class ItemPicture {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int pictureId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private ItemListing itemListing;

  @Column(nullable = false)
  private String fileName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImageFileType fileType;

  @Column(nullable = false)
  private String filePath;

  @Column(nullable = true)
  private String thumbnailPath;

  protected ItemPicture() {
  }

  public ItemPicture(String fileName, ImageFileType fileType, String filePath) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.filePath = filePath;
  }

  public ItemPicture(String fileName, ImageFileType fileType, String filePath, String thumbnailPath) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.filePath = filePath;
    this.thumbnailPath = thumbnailPath;
  }

  public int getPictureId() {
    return pictureId;
  }

  public ItemListing getItemListing() {
    return itemListing;
  }

  void setItemListing(ItemListing itemListing) {
    this.itemListing = itemListing;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public ImageFileType getFileType() {
    return fileType;
  }

  public void setFileType(ImageFileType fileType) {
    this.fileType = fileType;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getThumbnailPath() {
    return thumbnailPath;
  }

  public void setThumbnailPath(String thumbnailPath) {
    this.thumbnailPath = thumbnailPath;
  }
}