package dk.via.sep3.dao;

import dk.via.sep3.dto.ItemSummaryDataDTO;
import dk.via.sep3.entities.ItemListing;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
public class JpaItemSummarySearch implements ItemSummarySearch {

    private final EntityManager entityManager;

    public JpaItemSummarySearch(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemSummaryDataDTO> search(
            String title, List<String> categoryNames, Pageable page) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(ItemSummaryDataDTO.class);
        var item = query.from(ItemListing.class);
        List<Predicate> filters = new ArrayList<>();

        if (!categoryNames.isEmpty()) {
            var categoryMatch = query.subquery(Integer.class);
            var category = categoryMatch.correlate(item).join("categories");
            categoryMatch.select(cb.literal(1))
                    .where(category.get("name").in(categoryNames));
            filters.add(cb.exists(categoryMatch));
        }

        var titlePattern = cb.parameter(String.class, "titlePattern");
        if (!title.isEmpty()) {
            filters.add(cb.like(
                    cb.lower(item.get("itemName")), cb.lower(titlePattern), '!'));
        }

        query.select(cb.construct(ItemSummaryDataDTO.class,
                        item.get("itemId"), item.get("itemName"), item.get("description"),
                        item.get("price"), item.get("listDate"), item.get("ownerId")))
                .where(filters.toArray(Predicate[]::new))
                .orderBy(QueryUtils.toOrders(page.getSort(), item, cb));

        TypedQuery<ItemSummaryDataDTO> search = entityManager.createQuery(query);
        if (!title.isEmpty()) {
            search.setParameter(titlePattern, containsPattern(title));
        }

        return search.setFirstResult(Math.toIntExact(page.getOffset()))
                .setMaxResults(page.getPageSize())
                .getResultList();
    }

    private static String containsPattern(String title) {
        return "%" + title.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
