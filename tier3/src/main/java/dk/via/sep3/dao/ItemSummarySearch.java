package dk.via.sep3.dao;

import dk.via.sep3.dto.ItemSummaryDataDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Read-side contract for the catalogue search query. */
public interface ItemSummarySearch {

    /**
     * Apply the title and category criteria before sorting and paging. A blank
     * title matches all titles. A nonblank title is a case-insensitive literal
     * substring of the item name. An empty category list matches any category.
     * Otherwise, an item must have at least one of the requested categories.
     * Return one summary per listing, or an empty list if the page has no matches.
     */
    List<ItemSummaryDataDTO> search(String title, List<String> categoryNames, Pageable page);
}
