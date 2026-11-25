package dk.via.sep3.repository;

import dk.via.sep3.entities.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    @Query("select distinct c from Conversation c left join fetch c.messages m " +
            "left join fetch m.sender where c.id = :id")
    @EntityGraph(attributePaths = {"owner", "sender"})
    Optional<Conversation> findWithMessagesById(@Param("id") int id);

    @EntityGraph(attributePaths = {"messages", "messages.sender"})
    Optional<Conversation> findById(Integer id);

    @Query("select c from Conversation c where c.owner.memberId = :memberId " +
            "or c.sender.memberId = :memberId order by c.id desc")
    @EntityGraph(attributePaths = "itemListing")
    List<Conversation> findSummariesForMember(@Param("memberId") int memberId);
}