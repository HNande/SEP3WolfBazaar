package dk.via.sep3.repository;

import dk.via.sep3.entities.ItemPicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPictureRepository extends JpaRepository<ItemPicture, Integer> {

    @Query("""
            SELECT p, p.itemListing.itemId FROM ItemPicture p
            WHERE p.pictureId IN (
                SELECT MIN(p2.pictureId) FROM ItemPicture p2
                WHERE p2.itemListing.itemId IN :itemIds
                GROUP BY p2.itemListing.itemId)
            """)
    List<Object[]> findFirstPicturesByItemIds(@Param("itemIds") List<Integer> itemIds);
}
