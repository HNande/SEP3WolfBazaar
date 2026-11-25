package dk.via.sep3.mappers;

import com.google.protobuf.Timestamp;
import dk.via.sep3.dto.ConversationDataDTO;
import dk.via.sep3.dto.ConversationSummaryDataDTO;
import dk.via.sep3.dto.MessageDataDTO;
import dk.via.sep3.generated.ConversationResponse;
import dk.via.sep3.generated.ListConversationSummaryResponse;
import dk.via.sep3.generated.MessageResponse;

import java.util.List;

public final class ConversationDataMapper {

    private ConversationDataMapper() {
    }

    public static ConversationResponse toResponse(ConversationDataDTO dto) {
        return ConversationResponse.newBuilder()
                .setConversation(dk.via.sep3.generated.Conversation.newBuilder()
                        .setId(dto.id())
                        .addAllMessages(dto.messages().stream()
                                .map(ConversationDataMapper::toProto)
                                .toList())
                        .build())
                .build();
    }

    public static ListConversationSummaryResponse toSummaryResponse(List<ConversationSummaryDataDTO> summaries) {
        return ListConversationSummaryResponse.newBuilder()
                .addAllConversations(summaries.stream()
                        .map(ConversationDataMapper::toProto)
                        .toList())
                .build();
    }

    public static MessageResponse toResponse(MessageDataDTO dto) {
        return MessageResponse.newBuilder()
                .setMessage(toProto(dto))
                .build();
    }

    private static dk.via.sep3.generated.Message toProto(MessageDataDTO dto) {
        Timestamp createdAt = Timestamp.newBuilder()
                .setSeconds(dto.createdAt().getEpochSecond())
                .setNanos(dto.createdAt().getNano())
                .build();
        return dk.via.sep3.generated.Message.newBuilder()
                .setId(dto.id())
                .setConversationId(dto.conversationId())
                .setSenderId(dto.senderId())
                .setContent(dto.content())
                .setCreatedAt(createdAt)
                .build();
    }

    private static dk.via.sep3.generated.ConversationSummary toProto(ConversationSummaryDataDTO dto) {
        return dk.via.sep3.generated.ConversationSummary.newBuilder()
                .setId(dto.id())
                .setOwnerId(dto.ownerId())
                .setItemName(dto.itemName())
                .build();
    }
}