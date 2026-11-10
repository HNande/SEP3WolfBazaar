package dk.via.sep3.util;

import dk.via.sep3.entities.Category;
import dk.via.sep3.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CategorySeeder implements CommandLineRunner {
  private final CategoryRepository repository;

  public CategorySeeder(CategoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public void run(String... args) {
    for (String name : List.of("CLOTHING", "ACCESSORIES", "CURIOS")) {
      if (repository.findByName(name).isEmpty()) {
        repository.save(new Category(name));
      }
    }
  }
}