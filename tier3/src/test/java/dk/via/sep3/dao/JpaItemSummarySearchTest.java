package dk.via.sep3.dao;

import dk.via.sep3.entities.Category;
import dk.via.sep3.entities.ItemListing;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:item-summary-search;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(JpaItemSummarySearch.class)
class JpaItemSummarySearchTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JpaItemSummarySearch search;

    @Test
    void searchFiltersByTitleAndCategory() {
        Category clothing = category("CLOTHING");
        Category electronics = category("ELECTRONICS");
        ItemListing winterCoat = listing("filter-scope coat winter", 20, clothing);
        ItemListing summerCoat = listing("filter-scope coat summer", 30, clothing);
        listing("filter-scope coat electronics", 40, electronics);
        listing("filter-scope boots", 10, clothing);
        entityManager.flush();
        entityManager.clear();

        List<dk.via.sep3.dto.ItemSummaryDataDTO> results = search.search(
                "FILTER-SCOPE COAT", List.of("CLOTHING"),
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("itemId"))));

        assertEquals(List.of(winterCoat.getItemId(), summerCoat.getItemId()),
                results.stream().map(result -> result.itemId()).toList());
    }

    @Test
    void searchTreatsLikeWildcardsAsLiteralCharacters() {
        Category clothing = category("CLOTHING");
        ItemListing literalPercent = listing("wildcard-scope % sale", 20, clothing);
        listing("wildcard-scope X sale", 30, clothing);
        listing("wildcard-scope_under", 40, clothing);
        listing("wildcard-scopeAunder", 50, clothing);
        entityManager.flush();
        entityManager.clear();

        List<dk.via.sep3.dto.ItemSummaryDataDTO> results = search.search(
                "wildcard-scope %", List.of(),
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("itemId"))));

        assertEquals(List.of(literalPercent.getItemId()),
                results.stream().map(result -> result.itemId()).toList());
    }

    @Test
    void searchAppliesOffsetAndPageSize() {
        Category clothing = category("CLOTHING");
        listing("paging-scope coat one", 10, clothing);
        listing("paging-scope coat two", 20, clothing);
        ItemListing third = listing("paging-scope coat three", 30, clothing);
        ItemListing fourth = listing("paging-scope coat four", 40, clothing);
        entityManager.flush();
        entityManager.clear();

        List<dk.via.sep3.dto.ItemSummaryDataDTO> results = search.search(
                "paging-scope", List.of(), PageRequest.of(1, 2, Sort.by(Sort.Order.asc("itemId"))));

        assertEquals(List.of(third.getItemId(), fourth.getItemId()),
                results.stream().map(result -> result.itemId()).toList());
    }

    private Category category(String name) {
        Category category = new Category(name);
        entityManager.persist(category);
        return category;
    }

    private ItemListing listing(String name, int price, Category category) {
        ItemListing listing = new ItemListing(name, "Description", price, 3);
        listing.setListDate(LocalDate.of(2025, 1, 2));
        listing.getCategories().add(category);
        entityManager.persist(listing);
        return listing;
    }
}