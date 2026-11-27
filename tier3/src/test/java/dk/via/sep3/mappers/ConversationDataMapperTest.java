package dk.via.sep3.mappers;

import dk.via.sep3.dto.ConversationDataDTO;
import dk.via.sep3.dto.ConversationSummaryDataDTO;
import dk.via.sep3.dto.MessageDataDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationDataMapperTest {

    @Test
    void toResponseMapsConversationMessagesAndTimestampPrecisely() {
        MessageDataDTO message = new MessageDataDTO(
                9, 4, 3, "Hello", Instant.parse("2025-03-04T05:06:07.123456789Z"));

        var result = ConversationDataMapper.toResponse(new ConversationDataDTO(4, List.of(message)));

        assertEquals(4, result.getConversation().getId());
        assertEquals(1, result.getConversation().getMessagesCount());
        var mapped = result.getConversation().getMessages(0);
        assertEquals(9, mapped.getId());
        assertEquals(4, mapped.getConversationId());
        assertEquals(3, mapped.getSenderId());
        assertEquals("Hello", mapped.getContent());
        assertEquals(Instant.parse("2025-03-04T05:06:07.123456789Z").getEpochSecond(),
                mapped.getCreatedAt().getSeconds());
        assertEquals(123_456_789, mapped.getCreatedAt().getNanos());
    }

    @Test
    void toSummaryResponseMapsAllConversationSummaries() {
        var result = ConversationDataMapper.toSummaryResponse(List.of(
                new ConversationSummaryDataDTO(4, 2, "Coat"),
                new ConversationSummaryDataDTO(5, 3, "Table")));

        assertEquals(2, result.getConversationsCount());
        assertEquals(4, result.getConversations(0).getId());
        assertEquals(2, result.getConversations(0).getOwnerId());
        assertEquals("Coat", result.getConversations(0).getItemName());
        assertEquals("Table", result.getConversations(1).getItemName());
    }

    @Test
    void toMessageResponseMapsTheMessage() {
        var result = ConversationDataMapper.toResponse(
                new MessageDataDTO(9, 4, 3, "Hello", Instant.EPOCH));

        assertEquals(9, result.getMessage().getId());
        assertEquals(0, result.getMessage().getCreatedAt().getSeconds());
        assertEquals(0, result.getMessage().getCreatedAt().getNanos());
    }
}