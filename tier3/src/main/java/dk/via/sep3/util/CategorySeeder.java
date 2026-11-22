package dk.via.sep3.util;

import dk.via.sep3.entities.Category;
import dk.via.sep3.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class CategorySeeder implements CommandLineRunner {
  private final CategoryRepository repository;

  public CategorySeeder(CategoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public void run(String... args) {
    Arrays.stream(dk.via.sep3.generated.Category.values())
        .filter(c -> c != dk.via.sep3.generated.Category.UNSPECIFIED
            && c != dk.via.sep3.generated.Category.UNRECOGNIZED)
        .map(Enum::name)
        .filter(name -> repository.findByName(name).isEmpty())
        .forEach(name -> repository.save(new Category(name)));
  }
}