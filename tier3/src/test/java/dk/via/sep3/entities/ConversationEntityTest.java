package dk.via.sep3.entities;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationEntityTest {

    @Test
    void conversationIdIsGeneratedByTheDatabase() throws NoSuchFieldException {
        Field id = Conversation.class.getDeclaredField("id");

        assertNotNull(id.getAnnotation(Id.class));
        assertEquals(GenerationType.IDENTITY, id.getAnnotation(GeneratedValue.class).strategy());
    }

    @Test
    void listingAndNonOwnerParticipantIdentifyOneConversation() {
        Table table = Conversation.class.getAnnotation(Table.class);

        assertTrue(Arrays.stream(table.uniqueConstraints())
                .anyMatch(constraint -> Set.of(constraint.columnNames())
                        .equals(Set.of("item_listing_id", "sender_member_id"))));
    }

    @Test
    void messageTimestampIsTruncatedToMinutesBeforePersistence() {
        ConversationMessage message = new ConversationMessage();
        message.setCreatedAt(Instant.parse("2025-01-02T03:04:59.987654321Z"));

        message.onCreate();

        assertEquals(Instant.parse("2025-01-02T03:04:00Z"), message.getCreatedAt());
    }

    @Test
    void generatedMessageTimestampHasMinutePrecision() {
        ConversationMessage message = new ConversationMessage();

        message.onCreate();

        assertEquals(0, message.getCreatedAt().getEpochSecond() % 60);
        assertEquals(0, message.getCreatedAt().getNano());
    }
}