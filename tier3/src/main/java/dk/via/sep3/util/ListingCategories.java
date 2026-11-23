package dk.via.sep3.util;

import dk.via.sep3.generated.Category;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ListingCategories {

    private ListingCategories() {}

    public static List<String> supportedNames() {
        return Arrays.stream(Category.values())
                .filter(category -> category != Category.UNSPECIFIED
                        && category != Category.UNRECOGNIZED)
                .map(ListingCategories::name)
                .distinct()
                .toList();
    }

    public static List<String> names(List<Category> categories) {
        return categories.stream()
                .map(ListingCategories::name)
                .distinct()
                .toList();
    }

    public static List<String> searchNames(List<Category> categories) {
        return names(categories).stream()
                .flatMap(name -> switch (name) {
                    case "GARDENING" -> Stream.of(name, "OUTDOOR_GOODS");
                    case "CURIOSITIES" -> Stream.of(name, "CURIOS");
                    default -> Stream.of(name);
                })
                .toList();
    }

    public static Category fromStoredName(String name) {
        String normalized = name.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "OUTDOOR_GOODS" -> Category.GARDENING;
            case "CURIOS" -> Category.CURIOSITIES;
            default -> knownCategory(normalized);
        };
    }

    private static Category knownCategory(String name) {
        try {
            Category category = Category.valueOf(name);
            return category == Category.UNRECOGNIZED ? Category.UNSPECIFIED : category;
        } catch (IllegalArgumentException e) {
            return Category.UNSPECIFIED;
        }
    }

    private static String name(Category category) {
        return switch (category) {
            case UNSPECIFIED, UNRECOGNIZED ->
                    throw new IllegalArgumentException("Unknown listing category");
            case OUTDOOR_GOODS -> Category.GARDENING.name();
            default -> category.name();
        };
    }
}
