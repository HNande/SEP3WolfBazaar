package dk.via.sep3.service;

import dk.via.sep3.dao.ConversationDAO;
import dk.via.sep3.dto.ConversationDataDTO;
import dk.via.sep3.dto.ConversationSummaryDataDTO;
import dk.via.sep3.dto.MessageDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DataConversationServiceTest {

    private ConversationDAO conversationDAO;
    private DataConversationService service;

    @BeforeEach
    void setUp() {
        conversationDAO = mock(ConversationDAO.class);
        service = new DataConversationService(conversationDAO);
    }

    @Test
    void addConversation_delegatesToDao() {
        ConversationDataDTO expected = new ConversationDataDTO(7, List.of());
        when(conversationDAO.addConversation(2, 3, 4)).thenReturn(expected);

        assertEquals(expected, service.addConversation(2, 3, 4));
        verify(conversationDAO).addConversation(2, 3, 4);
    }

    @Test
    void getConversation_missingId_throwsNotFound() {
        when(conversationDAO.getConversation(7)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getConversation(7));
    }

    @Test
    void getConversationSummaryList_delegatesToDao() {
        List<ConversationSummaryDataDTO> expected = List.of(new ConversationSummaryDataDTO(7, 2, "Coat"));
        when(conversationDAO.getConversationSummaryList(2)).thenReturn(expected);

        assertEquals(expected, service.getConversationSummaryList(2));
        verify(conversationDAO).getConversationSummaryList(2);
    }

    @Test
    void addMessage_delegatesToDao() {
        MessageDataDTO expected = new MessageDataDTO(9, 7, 3, "Hello", Instant.EPOCH);
        when(conversationDAO.addMessage(7, 3, "Hello")).thenReturn(expected);

        assertEquals(expected, service.addMessage(7, 3, "Hello"));
        verify(conversationDAO).addMessage(7, 3, "Hello");
    }

    @Test
    void invalidIdsAndBlankMessage_areRejectedBeforeDaoCalls() {
        assertThrows(IllegalArgumentException.class, () -> service.addConversation(0, 3, 4));
        assertThrows(IllegalArgumentException.class, () -> service.getConversation(0));
        assertThrows(IllegalArgumentException.class, () -> service.removeConversation(0));
        assertThrows(IllegalArgumentException.class, () -> service.getConversationSummaryList(0));
        assertThrows(IllegalArgumentException.class, () -> service.addMessage(7, 3, " "));
        verifyNoInteractions(conversationDAO);
    }
}