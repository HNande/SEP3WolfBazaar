package dk.via.sep3.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "item_listing")
public class ItemListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private int itemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "list_date", nullable = false)
    private LocalDate listDate;

    @ManyToMany
    @JoinTable(
        name = "item_listing_category",
        joinColumns = @JoinColumn(name = "item_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "itemListing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("pictureId ASC")
    private List<ItemPicture> pictures = new ArrayList<>();

    public ItemListing() {
    }

    public ItemListing(String itemName, String description, int price) {
        this.itemName = itemName;
        this.description = description;
        this.price = price;
    }

    @PrePersist
    void onCreate() {
        if (listDate == null) {
            listDate = LocalDate.now();
        }
    }

    public void addPicture(ItemPicture picture) {
        pictures.add(picture);
        picture.setItemListing(this);
    }

    public void removePicture(ItemPicture picture) {
        pictures.remove(picture);
        picture.setItemListing(null);
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public LocalDate getListDate() { return listDate; }
    public void setListDate(LocalDate listDate) { this.listDate = listDate; }

    public List<ItemPicture> getPictures() { return pictures; }
    public void setPictures(List<ItemPicture> pictures) { this.pictures = pictures; }
}
