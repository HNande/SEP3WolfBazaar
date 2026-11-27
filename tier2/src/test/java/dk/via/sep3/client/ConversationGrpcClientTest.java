package dk.via.sep3.client;

import com.google.protobuf.Empty;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.generated.AddConversationRequest;
import dk.via.sep3.generated.AddMessageRequest;
import dk.via.sep3.generated.Conversation;
import dk.via.sep3.generated.ConversationResponse;
import dk.via.sep3.generated.ConversationServiceGrpc;
import dk.via.sep3.generated.ConversationSummary;
import dk.via.sep3.generated.GetConversationRequest;
import dk.via.sep3.generated.ListConversationSummaryResponse;
import dk.via.sep3.generated.ListConversationsSummaryRequest;
import dk.via.sep3.generated.Message;
import dk.via.sep3.generated.MessageResponse;
import dk.via.sep3.generated.RemoveConversationRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationGrpcClientTest {

    private ConversationServiceGrpc.ConversationServiceBlockingStub stub;
    private ConversationGrpcClient client;

    @BeforeEach
    void setUp() {
        stub = mock(ConversationServiceGrpc.ConversationServiceBlockingStub.class);
        client = new ConversationGrpcClient(stub);
        when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);
    }

    @Test
    void addConversationBuildsRequestAndReturnsConversation() {
        Conversation expected = Conversation.newBuilder().setId(8).build();
        when(stub.addConversation(any(AddConversationRequest.class)))
                .thenReturn(ConversationResponse.newBuilder().setConversation(expected).build());

        assertEquals(expected, client.addConversation(AddConversationRequest.newBuilder()
                .setOwnerId(2).setSenderId(3).setItemListingId(4).build()));
        ArgumentCaptor<AddConversationRequest> request = ArgumentCaptor.forClass(AddConversationRequest.class);
        verify(stub).addConversation(request.capture());
        assertEquals(2, request.getValue().getOwnerId());
        assertEquals(3, request.getValue().getSenderId());
        assertEquals(4, request.getValue().getItemListingId());
    }

    @Test
    void getConversationBuildsRequestAndReturnsOptionalValue() {
        Conversation expected = Conversation.newBuilder().setId(8).build();
        when(stub.getConversation(any(GetConversationRequest.class)))
                .thenReturn(ConversationResponse.newBuilder().setConversation(expected).build());

        assertEquals(Optional.of(expected), client.getConversation(8));
        ArgumentCaptor<GetConversationRequest> request = ArgumentCaptor.forClass(GetConversationRequest.class);
        verify(stub).getConversation(request.capture());
        assertEquals(8, request.getValue().getId());
    }

    @Test
    void notFoundConversationAndRemovalReturnEmptyAndFalse() {
        when(stub.getConversation(any(GetConversationRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.NOT_FOUND));
        when(stub.removeConversation(any(RemoveConversationRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

        assertEquals(Optional.empty(), client.getConversation(8));
        assertFalse(client.removeConversation(8));
    }

    @Test
    void removeConversationBuildsRequestAndReturnsTrue() {
        when(stub.removeConversation(any(RemoveConversationRequest.class))).thenReturn(Empty.getDefaultInstance());

        assertTrue(client.removeConversation(8));
        ArgumentCaptor<RemoveConversationRequest> request = ArgumentCaptor.forClass(RemoveConversationRequest.class);
        verify(stub).removeConversation(request.capture());
        assertEquals(8, request.getValue().getConversationId());
    }

    @Test
    void summaryListMapsResponseAndBuildsMemberRequest() {
        List<ConversationSummary> expected = List.of(
                ConversationSummary.newBuilder().setId(4).setOwnerId(2).setItemName("Coat").build());
        when(stub.getConversationSummaryList(any(ListConversationsSummaryRequest.class)))
                .thenReturn(ListConversationSummaryResponse.newBuilder().addAllConversations(expected).build());

        assertEquals(expected, client.getConversationSummaryList(2));
        ArgumentCaptor<ListConversationsSummaryRequest> request =
                ArgumentCaptor.forClass(ListConversationsSummaryRequest.class);
        verify(stub).getConversationSummaryList(request.capture());
        assertEquals(2, request.getValue().getMemberId());
    }

    @Test
    void addMessageBuildsRequestAndReturnsMessage() {
        Message expected = Message.newBuilder().setId(5).setContent("Hello").build();
        when(stub.addMessage(any(AddMessageRequest.class)))
                .thenReturn(MessageResponse.newBuilder().setMessage(expected).build());

        assertEquals(expected, client.addMessage(AddMessageRequest.newBuilder()
                .setConversationId(8).setSenderId(3).setContent("Hello").build()));
        ArgumentCaptor<AddMessageRequest> request = ArgumentCaptor.forClass(AddMessageRequest.class);
        verify(stub).addMessage(request.capture());
        assertEquals(8, request.getValue().getConversationId());
        assertEquals(3, request.getValue().getSenderId());
        assertEquals("Hello", request.getValue().getContent());
    }

    @Test
    void invalidArgumentUsesUpstreamDescriptionAndOtherStatusesPreserveCause() {
        when(stub.addConversation(any(AddConversationRequest.class)))
                .thenThrow(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Owner mismatch")));
        InvalidRequestException invalid = assertThrows(InvalidRequestException.class,
                () -> client.addConversation(AddConversationRequest.getDefaultInstance()));
        assertEquals("Owner mismatch", invalid.getMessage());

        StatusRuntimeException grpcFailure = new StatusRuntimeException(Status.UNAVAILABLE);
        when(stub.addMessage(any(AddMessageRequest.class))).thenThrow(grpcFailure);
        UpstreamException upstream = assertThrows(UpstreamException.class,
                () -> client.addMessage(AddMessageRequest.getDefaultInstance()));
        assertTrue(upstream.getMessage().contains("add message"));
        assertTrue(upstream.getMessage().contains("UNAVAILABLE"));
        assertEquals(grpcFailure, upstream.getCause());
    }
}