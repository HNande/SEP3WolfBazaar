package dk.via.sep3.dto;

import java.util.List;

public record ItemSearchCriteriaDTO(
        String title,
        List<CategoryJsonDTO> categories,
        boolean orderByPrice,
        boolean orderByDate,
        int counter
) {
    public ItemSearchCriteriaDTO {
        title = title == null ? "" : title.trim();
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
