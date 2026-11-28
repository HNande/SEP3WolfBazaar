package dk.via.sep3.server;

import dk.via.sep3.dto.ConversationDataDTO;
import dk.via.sep3.dto.ConversationSummaryDataDTO;
import dk.via.sep3.dto.MessageDataDTO;
import dk.via.sep3.service.DataConversationService;
import dk.via.sep3.support.RecordingStreamObserver;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrpcConversationServerTest {

    private DataConversationService service;
    private GrpcConversationServer server;

    @BeforeEach
    void setUp() {
        service = mock(DataConversationService.class);
        server = new GrpcConversationServer(service);
    }

    @Test
    void addConversationMapsRequestAndResponse() {
        when(service.addConversation(2, 3, 4)).thenReturn(new ConversationDataDTO(12, List.of()));
        var request = dk.via.sep3.generated.AddConversationRequest.newBuilder()
                .setOwnerId(2).setSenderId(3).setItemListingId(4).build();
        RecordingStreamObserver<dk.via.sep3.generated.ConversationResponse> observer = new RecordingStreamObserver<>();

        server.addConversation(request, observer);

        assertNull(observer.error());
        assertTrue(observer.completed());
        assertEquals(12, observer.values().get(0).getConversation().getId());
        verify(service).addConversation(2, 3, 4);
    }

    @Test
    void getConversationMapsMissingEntityToNotFound() {
        when(service.getConversation(7)).thenThrow(new NoSuchElementException("Conversation not found"));
        RecordingStreamObserver<dk.via.sep3.generated.ConversationResponse> observer = new RecordingStreamObserver<>();

        server.getConversation(dk.via.sep3.generated.GetConversationRequest.newBuilder().setId(7).build(), observer);

        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(observer.error()).getCode());
        assertEquals("Conversation not found", Status.fromThrowable(observer.error()).getDescription());
        assertFalse(observer.completed());
        assertTrue(observer.values().isEmpty());
    }

    @Test
    void removeConversationReturnsEmptyOrNotFound() {
        when(service.removeConversation(12)).thenReturn(true);
        RecordingStreamObserver<com.google.protobuf.Empty> removed = new RecordingStreamObserver<>();
        server.removeConversation(dk.via.sep3.generated.RemoveConversationRequest.newBuilder()
                .setConversationId(12).build(), removed);

        assertTrue(removed.completed());
        assertEquals(1, removed.values().size());

        when(service.removeConversation(13)).thenReturn(false);
        RecordingStreamObserver<com.google.protobuf.Empty> missing = new RecordingStreamObserver<>();
        server.removeConversation(dk.via.sep3.generated.RemoveConversationRequest.newBuilder()
                .setConversationId(13).build(), missing);
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(missing.error()).getCode());
    }

    @Test
    void summariesAndMessagesMapToTheirGrpcResponses() {
        when(service.getConversationSummaryList(2)).thenReturn(List.of(
                new ConversationSummaryDataDTO(12, 2, "Coat")));
        RecordingStreamObserver<dk.via.sep3.generated.ListConversationSummaryResponse> summaries =
                new RecordingStreamObserver<>();
        server.getConversationSummaryList(dk.via.sep3.generated.ListConversationsSummaryRequest.newBuilder()
                .setMemberId(2).build(), summaries);
        assertEquals("Coat", summaries.values().get(0).getConversations(0).getItemName());
        assertTrue(summaries.completed());

        when(service.addMessage(12, 3, "Hello"))
                .thenReturn(new MessageDataDTO(8, 12, 3, "Hello", Instant.EPOCH));
        RecordingStreamObserver<dk.via.sep3.generated.MessageResponse> message = new RecordingStreamObserver<>();
        server.addMessage(dk.via.sep3.generated.AddMessageRequest.newBuilder()
                .setConversationId(12).setSenderId(3).setContent("Hello").build(), message);
        assertEquals("Hello", message.values().get(0).getMessage().getContent());
        assertTrue(message.completed());
    }

    @Test
    void invalidArgumentsAndUnexpectedFailuresUseGrpcStatusCodes() {
        when(service.getConversation(0)).thenThrow(new IllegalArgumentException("Invalid id"));
        RecordingStreamObserver<dk.via.sep3.generated.ConversationResponse> invalid = new RecordingStreamObserver<>();
        server.getConversation(dk.via.sep3.generated.GetConversationRequest.newBuilder().setId(0).build(), invalid);
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalid.error()).getCode());

        when(service.getConversation(10)).thenThrow(new IllegalStateException("Database unavailable"));
        RecordingStreamObserver<dk.via.sep3.generated.ConversationResponse> failed = new RecordingStreamObserver<>();
        server.getConversation(dk.via.sep3.generated.GetConversationRequest.newBuilder().setId(10).build(), failed);
        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(failed.error()).getCode());
    }
}