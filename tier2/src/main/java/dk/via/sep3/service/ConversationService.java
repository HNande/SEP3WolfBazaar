package dk.via.sep3.service;

import dk.via.sep3.client.ConversationGrpcClient;
import dk.via.sep3.dto.ConversationResponseDTO;
import dk.via.sep3.dto.ConversationSummaryResponseDTO;
import dk.via.sep3.dto.MessageResponseDTO;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.generated.AddConversationRequest;
import dk.via.sep3.generated.AddMessageRequest;
import dk.via.sep3.generated.Conversation;
import dk.via.sep3.generated.ConversationSummary;
import dk.via.sep3.generated.Message;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ConversationService {

    private final ConversationGrpcClient client;

    public ConversationService(ConversationGrpcClient client) {
        this.client = client;
    }

    public ConversationResponseDTO addConversation(int ownerId, int senderId, int itemListingId) {
        validatePositiveId(ownerId, "Owner id");
        validatePositiveId(senderId, "Sender id");
        validatePositiveId(itemListingId, "Item listing id");

        AddConversationRequest request = AddConversationRequest.newBuilder()
                .setOwnerId(ownerId)
                .setSenderId(senderId)
                .setItemListingId(itemListingId)
                .build();
        return toResponse(client.addConversation(request));
    }

    public ConversationResponseDTO getConversation(int id) {
        validatePositiveId(id, "Conversation id");
        return client.getConversation(id)
                .map(ConversationService::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Conversation " + id + " not found"));
    }

    public boolean removeConversation(int id) {
        validatePositiveId(id, "Conversation id");
        return client.removeConversation(id);
    }

    public List<ConversationSummaryResponseDTO> getConversationSummaryList(int memberId) {
        validatePositiveId(memberId, "Member id");
        return client.getConversationSummaryList(memberId).stream()
                .map(ConversationService::toSummaryResponse)
                .toList();
    }

    public MessageResponseDTO addMessage(int conversationId, int senderId, String content) {
        validatePositiveId(conversationId, "Conversation id");
        validatePositiveId(senderId, "Sender id");
        if (content == null || content.isBlank()) {
            throw new InvalidRequestException("Message content must not be blank");
        }

        AddMessageRequest request = AddMessageRequest.newBuilder()
                .setConversationId(conversationId)
                .setSenderId(senderId)
                .setContent(content)
                .build();
        return toResponse(client.addMessage(request));
    }

    private static void validatePositiveId(int id, String label) {
        if (id <= 0) {
            throw new InvalidRequestException(label + " must be a positive integer");
        }
    }

    private static ConversationResponseDTO toResponse(Conversation conversation) {
        List<MessageResponseDTO> messages = conversation.getMessagesList().stream()
                .map(ConversationService::toResponse)
                .toList();
        return new ConversationResponseDTO(conversation.getId(), messages);
    }

    private static MessageResponseDTO toResponse(Message message) {
        Instant createdAt = Instant.ofEpochSecond(
                message.getCreatedAt().getSeconds(), message.getCreatedAt().getNanos())
                .truncatedTo(ChronoUnit.MINUTES);
        return new MessageResponseDTO(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getContent(),
                createdAt);
    }

    private static ConversationSummaryResponseDTO toSummaryResponse(ConversationSummary summary) {
        return new ConversationSummaryResponseDTO(
                summary.getId(),
                summary.getOwnerId(),
                summary.getItemName());
    }
}