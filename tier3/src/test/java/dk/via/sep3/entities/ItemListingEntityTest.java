package dk.via.sep3.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ItemListingEntityTest {

    @Test
    void constructorSetsFieldsAndStartsWithNoPicturesOrCategories() {
        ItemListing listing = new ItemListing("Coat", "Warm", 250, 4);

        assertThat(listing.getItemName()).isEqualTo("Coat");
        assertThat(listing.getDescription()).isEqualTo("Warm");
        assertThat(listing.getPrice()).isEqualTo(250);
        assertThat(listing.getOwnerId()).isEqualTo(4);
        assertThat(listing.getPictures()).isEmpty();
        assertThat(listing.getCategories()).isEmpty();
    }

    @Test
    void addPictureLinksBothSides() {
        ItemListing listing = new ItemListing();
        ItemPicture picture = new ItemPicture("a.png", ImageFileType.PNG, "/tmp/a.png");

        listing.addPicture(picture);

        assertThat(listing.getPictures()).containsExactly(picture);
        assertThat(picture.getItemListing()).isSameAs(listing);
    }

    @Test
    void removePictureUnlinksBothSides() {
        ItemListing listing = new ItemListing();
        ItemPicture picture = new ItemPicture("a.png", ImageFileType.PNG, "/tmp/a.png");
        listing.addPicture(picture);

        listing.removePicture(picture);

        assertThat(listing.getPictures()).isEmpty();
        assertThat(picture.getItemListing()).isNull();
    }

    @Test
    void onCreateDefaultsListDateToToday() {
        ItemListing listing = new ItemListing();

        listing.onCreate();

        assertThat(listing.getListDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void onCreateKeepsAnExplicitListDate() {
        ItemListing listing = new ItemListing();
        LocalDate date = LocalDate.of(2024, 5, 6);
        listing.setListDate(date);

        listing.onCreate();

        assertThat(listing.getListDate()).isEqualTo(date);
    }
}
