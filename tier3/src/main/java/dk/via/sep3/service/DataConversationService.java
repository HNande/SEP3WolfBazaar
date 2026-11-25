package dk.via.sep3.service;

import dk.via.sep3.dao.ConversationDAO;
import dk.via.sep3.dto.ConversationDataDTO;
import dk.via.sep3.dto.ConversationSummaryDataDTO;
import dk.via.sep3.dto.MessageDataDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DataConversationService {

    private final ConversationDAO conversationDAO;

    public DataConversationService(ConversationDAO conversationDAO) {
        this.conversationDAO = conversationDAO;
    }

    public ConversationDataDTO addConversation(int ownerId, int senderId, int itemListingId) {
        validatePositiveId(ownerId, "Owner id");
        validatePositiveId(senderId, "Sender id");
        validatePositiveId(itemListingId, "Item listing id");
        return conversationDAO.addConversation(ownerId, senderId, itemListingId);
    }

    public ConversationDataDTO getConversation(int id) {
        validatePositiveId(id, "Conversation id");
        return conversationDAO.getConversation(id)
                .orElseThrow(() -> new NoSuchElementException("Conversation " + id + " not found"));
    }

    public boolean removeConversation(int id) {
        validatePositiveId(id, "Conversation id");
        return conversationDAO.removeConversation(id);
    }

    public List<ConversationSummaryDataDTO> getConversationSummaryList(int memberId) {
        validatePositiveId(memberId, "Member id");
        return conversationDAO.getConversationSummaryList(memberId);
    }

    public MessageDataDTO addMessage(int conversationId, int senderId, String content) {
        validatePositiveId(conversationId, "Conversation id");
        validatePositiveId(senderId, "Sender id");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }
        return conversationDAO.addMessage(conversationId, senderId, content);
    }

    private static void validatePositiveId(int id, String label) {
        if (id <= 0) {
            throw new IllegalArgumentException(label + " must be a positive integer");
        }
    }
}