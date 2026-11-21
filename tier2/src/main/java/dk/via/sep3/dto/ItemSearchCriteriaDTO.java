package dk.via.sep3.dto;

import dk.via.sep3.generated.Category;

import java.util.List;

public record ItemSearchCriteriaDTO(
        String title,
        //Changed this to just use the generated category as it is not different at all from the CategoryJsonDto it used before
        List<Category> categories,
        int orderByPrice,
        int orderByDate,
        int counter
) {
    public ItemSearchCriteriaDTO {
        title = title == null ? "" : title.trim();
        //What the hell
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
