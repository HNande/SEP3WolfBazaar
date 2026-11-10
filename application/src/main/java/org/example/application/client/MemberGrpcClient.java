package org.example.application.client;

import generated.AddMemberRequest;
import generated.MemberServiceGrpc.MemberServiceBlockingStub;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.example.application.exception.InvalidRequestException;
import org.example.application.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MemberGrpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(MemberGrpcClient.class);
    private static final long TIMEOUT_SECONDS = 10;

    private final MemberServiceBlockingStub stub;

    public MemberGrpcClient(MemberServiceBlockingStub stub) {
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