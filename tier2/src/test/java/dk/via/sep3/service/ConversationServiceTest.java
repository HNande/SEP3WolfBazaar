package dk.via.sep3.service;

import com.google.protobuf.Timestamp;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private ConversationGrpcClient client;
    private ConversationService service;

    @BeforeEach
    void setUp() {
        client = mock(ConversationGrpcClient.class);
        service = new ConversationService(client);
    }

    @Test
    void addConversation_buildsRequestAndMapsResponse() {
        Conversation conversation = Conversation.newBuilder().setId(11).build();
        when(client.addConversation(any(AddConversationRequest.class))).thenReturn(conversation);

        ConversationResponseDTO result = service.addConversation(2, 3, 4);

        assertEquals(11, result.id());
        ArgumentCaptor<AddConversationRequest> request = ArgumentCaptor.forClass(AddConversationRequest.class);
        verify(client).addConversation(request.capture());
        assertEquals(2, request.getValue().getOwnerId());
        assertEquals(3, request.getValue().getSenderId());
        assertEquals(4, request.getValue().getItemListingId());
    }

    @Test
    void getConversation_mapsMessagesAndTimestamp() {
        Message message = Message.newBuilder()
                .setId(7)
                .setConversationId(11)
                .setSenderId(3)
                .setContent("Hello")
                .setCreatedAt(Timestamp.newBuilder().setSeconds(1_700_000_000).setNanos(123_000_000))
                .build();
        when(client.getConversation(11)).thenReturn(Optional.of(
                Conversation.newBuilder().setId(11).addMessages(message).build()));

        ConversationResponseDTO result = service.getConversation(11);

        assertEquals(11, result.id());
        assertEquals(List.of(new MessageResponseDTO(
                7, 11, 3, "Hello", Instant.parse("2023-11-14T22:13:00Z"))), result.messages());
    }

    @Test
    void getConversationSummaryList_mapsSummaries() {
        when(client.getConversationSummaryList(2)).thenReturn(List.of(
                ConversationSummary.newBuilder()
                        .setId(11)
                        .setOwnerId(2)
                        .setItemName("Vintage coat")
                        .build()));

        List<ConversationSummaryResponseDTO> result = service.getConversationSummaryList(2);

        assertEquals(List.of(new ConversationSummaryResponseDTO(11, 2, "Vintage coat")), result);
    }

    @Test
    void addMessage_buildsRequestAndMapsResponse() {
        Message message = Message.newBuilder()
                .setId(7)
                .setConversationId(11)
                .setSenderId(3)
                .setContent("Hello")
                .build();
        when(client.addMessage(any(AddMessageRequest.class))).thenReturn(message);

        MessageResponseDTO result = service.addMessage(11, 3, "Hello");

        assertEquals(7, result.id());
        assertEquals("Hello", result.content());
        ArgumentCaptor<AddMessageRequest> request = ArgumentCaptor.forClass(AddMessageRequest.class);
        verify(client).addMessage(request.capture());
        assertEquals(11, request.getValue().getConversationId());
        assertEquals(3, request.getValue().getSenderId());
        assertEquals("Hello", request.getValue().getContent());
    }

    @Test
    void getConversation_missingId_throwsNotFound() {
        when(client.getConversation(11)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getConversation(11));
    }

    @Test
    void addMessage_blankContent_isRejected() {
        assertThrows(InvalidRequestException.class, () -> service.addMessage(11, 3, " "));
        verifyNoInteractions(client);
    }
}