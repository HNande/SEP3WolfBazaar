package dk.via.sep3.server;

import com.google.protobuf.Empty;
import dk.via.sep3.generated.AddConversationRequest;
import dk.via.sep3.generated.AddMessageRequest;
import dk.via.sep3.generated.ConversationServiceGrpc;
import dk.via.sep3.generated.GetConversationRequest;
import dk.via.sep3.generated.ListConversationsSummaryRequest;
import dk.via.sep3.generated.RemoveConversationRequest;
import dk.via.sep3.mappers.ConversationDataMapper;
import dk.via.sep3.service.DataConversationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.util.NoSuchElementException;

@GrpcService
public class GrpcConversationServer extends ConversationServiceGrpc.ConversationServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcConversationServer.class);
    private final DataConversationService service;

    public GrpcConversationServer(DataConversationService service) {
        this.service = service;
    }

    @Override
    public void addConversation(AddConversationRequest request,
                               StreamObserver<dk.via.sep3.generated.ConversationResponse> responseObserver) {
        try {
            responseObserver.onNext(ConversationDataMapper.toResponse(service.addConversation(
                    request.getOwnerId(), request.getSenderId(), request.getItemListingId())));
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "add conversation");
        }
    }

    @Override
    public void getConversation(GetConversationRequest request,
                                StreamObserver<dk.via.sep3.generated.ConversationResponse> responseObserver) {
        try {
            responseObserver.onNext(ConversationDataMapper.toResponse(
                    service.getConversation(request.getId())));
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "get conversation");
        }
    }

    @Override
    public void removeConversation(RemoveConversationRequest request,
                                   StreamObserver<Empty> responseObserver) {
        try {
            if (!service.removeConversation(request.getConversationId())) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Conversation " + request.getConversationId() + " not found")
                        .asRuntimeException());
                return;
            }
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "remove conversation");
        }
    }

    @Override
    public void getConversationSummaryList(ListConversationsSummaryRequest request,
                                          StreamObserver<dk.via.sep3.generated.ListConversationSummaryResponse>
                                                  responseObserver) {
        try {
            responseObserver.onNext(ConversationDataMapper.toSummaryResponse(
                    service.getConversationSummaryList(request.getMemberId())));
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "list conversation summaries");
        }
    }

    @Override
    public void addMessage(AddMessageRequest request,
                           StreamObserver<dk.via.sep3.generated.MessageResponse> responseObserver) {
        try {
            responseObserver.onNext(ConversationDataMapper.toResponse(service.addMessage(
                    request.getConversationId(), request.getSenderId(), request.getContent())));
            responseObserver.onCompleted();
        } catch (Exception e) {
            fail(responseObserver, e, "add message");
        }
    }

    private void fail(StreamObserver<?> responseObserver, Exception e, String action) {
        NoSuchElementException missing = findCause(e, NoSuchElementException.class);
        if (missing != null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(missing.getMessage())
                    .asRuntimeException());
            return;
        }

        IllegalArgumentException invalid = findCause(e, IllegalArgumentException.class);
        if (invalid != null) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(invalid.getMessage())
                    .asRuntimeException());
            return;
        }

        LOG.error("Could not {}", action, e);
        responseObserver.onError(Status.INTERNAL
                .withDescription("Could not " + action)
                .asRuntimeException());
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        for (int depth = 0; throwable != null && depth < 10; depth++, throwable = throwable.getCause()) {
            if (type.isInstance(throwable)) {
                return type.cast(throwable);
            }
        }
        return null;
    }
}