package dk.via.sep3.client;

import dk.via.sep3.generated.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MemberGrpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(MemberGrpcClient.class);
    private static final int TIMEOUT_SECONDS = 10;

    private final MemberServiceGrpc.MemberServiceBlockingStub stub;

    public MemberGrpcClient(MemberServiceGrpc.MemberServiceBlockingStub stub) {
        this.stub = stub;
    }
    public boolean addMember(AddMemberRequest request) {
        try {
            stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .addMember(request);
            return true;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                return false;
            }
            throw translate(e, "add member");
        }
    }

    public boolean login(LoginRequest request) {
        try {
            stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .login(request);
            return true;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return false;
            }
            throw translate(e, "login");
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