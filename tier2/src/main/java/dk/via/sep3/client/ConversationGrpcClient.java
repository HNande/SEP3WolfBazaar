package dk.via.sep3.client;

import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.generated.AddConversationRequest;
import dk.via.sep3.generated.AddMessageRequest;
import dk.via.sep3.generated.Conversation;
import dk.via.sep3.generated.ConversationServiceGrpc;
import dk.via.sep3.generated.ConversationSummary;
import dk.via.sep3.generated.GetConversationRequest;
import dk.via.sep3.generated.ListConversationsSummaryRequest;
import dk.via.sep3.generated.Message;
import dk.via.sep3.generated.RemoveConversationRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class ConversationGrpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationGrpcClient.class);
    private static final int TIMEOUT_SECONDS = 10;

    private final ConversationServiceGrpc.ConversationServiceBlockingStub stub;

    public ConversationGrpcClient(ConversationServiceGrpc.ConversationServiceBlockingStub stub) {
        this.stub = stub;
    }

    public Conversation addConversation(AddConversationRequest request) {
        try {
            return stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .addConversation(request)
                    .getConversation();
        } catch (StatusRuntimeException e) {
            throw translate(e, "add conversation");
        }
    }

    public Optional<Conversation> getConversation(int id) {
        try {
            GetConversationRequest request = GetConversationRequest.newBuilder()
                    .setId(id)
                    .build();
            return Optional.of(stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getConversation(request)
                    .getConversation());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw translate(e, "get conversation " + id);
        }
    }

    public boolean removeConversation(int id) {
        try {
            RemoveConversationRequest request = RemoveConversationRequest.newBuilder()
                    .setConversationId(id)
                    .build();
            stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .removeConversation(request);
            return true;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return false;
            }
            throw translate(e, "remove conversation " + id);
        }
    }

    public List<ConversationSummary> getConversationSummaryList(int memberId) {
        try {
            ListConversationsSummaryRequest request = ListConversationsSummaryRequest.newBuilder()
                    .setMemberId(memberId)
                    .build();
            return stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getConversationSummaryList(request)
                    .getConversationsList();
        } catch (StatusRuntimeException e) {
            throw translate(e, "list conversation summaries");
        }
    }

    public Message addMessage(AddMessageRequest request) {
        try {
            return stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .addMessage(request)
                    .getMessage();
        } catch (StatusRuntimeException e) {
            throw translate(e, "add message");
        }
    }

    private RuntimeException translate(StatusRuntimeException e, String action) {
        Status status = e.getStatus();
        if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
            String message = status.getDescription() != null && !status.getDescription().isBlank()
                    ? status.getDescription()
                    : "Invalid request";
            LOG.warn("Tier 3 rejected '{}': {}", action, message);
            return new InvalidRequestException(message);
        }
        LOG.error("Could not {}. gRPC status: {}", action, status);
        return new UpstreamException("Could not " + action + ": " + status.getCode(), e);
    }
}