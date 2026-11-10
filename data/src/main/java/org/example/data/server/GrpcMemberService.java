package org.example.data.server;

import com.google.protobuf.Empty;
import generated.AddMemberRequest;
import generated.MemberServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.example.data.service.DataMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class GrpcMemberService extends MemberServiceGrpc.MemberServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcMemberService.class);
    private final DataMemberService dataMemberService;

    public GrpcMemberService(DataMemberService dataMemberService) {
        this.dataMemberService = dataMemberService;
    }

    @Override
    public void addMember(AddMemberRequest request, StreamObserver<Empty> responseObserver) {
        try {
            boolean created = dataMemberService.addMember(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getMemberName(),
                    request.getPassword()
            );
            if (created) {
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
                LOG.info("Successfully added Member: {}", request.getMemberName());
            } else {
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Username is already taken").asRuntimeException());
                LOG.warn("Registration failed: Username is already taken");
            }

        } catch(Exception e)
            {
                LOG.error("Failed to register member", e);
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Internal server error during registration")
                        .asRuntimeException());
            }
        }
    }

