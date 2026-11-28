package dk.via.sep3.util;

import dk.via.sep3.generated.Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingCategoriesTest {

    @Test
    void supportedNamesExcludeSentinelsAndTheOutdoorGoodsAlias() {
        List<String> names = ListingCategories.supportedNames();

        assertThat(names)
                .contains("CLOTHING", "GARDENING", "CURIOSITIES", "PET_SUPPLIES")
                .doesNotContain("UNSPECIFIED", "UNRECOGNIZED", "OUTDOOR_GOODS")
                .doesNotHaveDuplicates();
    }

    @Test
    void namesCollapseOutdoorGoodsIntoGardeningAndRemoveDuplicates() {
        List<String> names = ListingCategories.names(
                List.of(Category.CLOTHING, Category.OUTDOOR_GOODS, Category.GARDENING, Category.CLOTHING));

        assertThat(names).containsExactly("CLOTHING", "GARDENING");
    }

    @Test
    void namesRejectSentinelCategories() {
        assertThatThrownBy(() -> ListingCategories.names(List.of(Category.UNSPECIFIED)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListingCategories.names(List.of(Category.UNRECOGNIZED)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchNamesIncludeLegacyStoredNames() {
        assertThat(ListingCategories.searchNames(List.of(Category.OUTDOOR_GOODS)))
                .containsExactly("GARDENING", "OUTDOOR_GOODS");
        assertThat(ListingCategories.searchNames(List.of(Category.CURIOSITIES)))
                .containsExactly("CURIOSITIES", "CURIOS");
        assertThat(ListingCategories.searchNames(List.of(Category.CLOTHING)))
                .containsExactly("CLOTHING");
    }

    @Test
    void searchNamesOfEmptyListIsEmpty() {
        assertThat(ListingCategories.searchNames(List.of())).isEmpty();
    }

    @Test
    void fromStoredNameMapsLegacyAndCurrentNames() {
        assertThat(ListingCategories.fromStoredName("OUTDOOR_GOODS")).isEqualTo(Category.GARDENING);
        assertThat(ListingCategories.fromStoredName("CURIOS")).isEqualTo(Category.CURIOSITIES);
        assertThat(ListingCategories.fromStoredName(" electronics ")).isEqualTo(Category.ELECTRONICS);
    }

    @Test
    void fromStoredNameReturnsUnspecifiedForUnknownNames() {
        assertThat(ListingCategories.fromStoredName("SPACESHIPS")).isEqualTo(Category.UNSPECIFIED);
        assertThat(ListingCategories.fromStoredName("UNRECOGNIZED")).isEqualTo(Category.UNSPECIFIED);
    }
}
