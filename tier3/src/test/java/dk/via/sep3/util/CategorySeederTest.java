package dk.via.sep3.util;

import dk.via.sep3.entities.Category;
import dk.via.sep3.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategorySeederTest {

    @Test
    void seedsEveryRealProtoCategoryThatIsMissing() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.findByName(anyString())).thenReturn(Optional.empty());

        new CategorySeeder(repository).run();

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(repository, atLeastOnce()).save(saved.capture());
        long expected = Arrays.stream(dk.via.sep3.generated.Category.values())
                .filter(c -> c != dk.via.sep3.generated.Category.UNSPECIFIED
                        && c != dk.via.sep3.generated.Category.UNRECOGNIZED)
                .count();
        assertThat(saved.getAllValues())
                .extracting(Category::getName)
                .hasSize((int) expected)
                .contains("CLOTHING", "GARDENING", "OUTDOOR_GOODS")
                .doesNotContain("UNSPECIFIED", "UNRECOGNIZED");
    }

    @Test
    void doesNotSaveCategoriesThatAlreadyExist() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.findByName(anyString())).thenReturn(Optional.of(new Category("existing")));

        new CategorySeeder(repository).run();

        verify(repository, never()).save(any());
    }
}
