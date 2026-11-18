package dk.via.sep3.server;

import com.google.protobuf.Empty;
import dk.via.sep3.generated.AddMemberRequest;
import dk.via.sep3.generated.LoginRequest;
import dk.via.sep3.generated.MemberResponse;
import dk.via.sep3.generated.MemberServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import dk.via.sep3.service.DataMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Optional;

@GrpcService
public class GrpcMemberServer extends MemberServiceGrpc.MemberServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcMemberServer.class);
    private final DataMemberService dataMemberService;

    public GrpcMemberServer(DataMemberService dataMemberService) {
        this.dataMemberService = dataMemberService;
    }

    public void addMember(AddMemberRequest request, StreamObserver<MemberResponse> responseObserver) {
        try {

            Optional<dk.via.sep3.entities.Member> createdMember = dataMemberService.addMember(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getMemberName(),
                    request.getPassword(),
                    request.getMemberEmail()
            );
            LOG.info("Successfully added Member: {}", request.getMemberName());
            //if (responseObserver.onError(Status.ALREADY_EXISTS)) {
            //
            //}
            dk.via.sep3.entities.Member member = createdMember.get();
            dk.via.sep3.generated.Member protoMember = dk.via.sep3.generated.Member.newBuilder()
                .setFirstName(member.getFirstName())
                .setLastName(member.getLastName())
                .setMemberName(member.getUsername())
                .setId(member.getMemberId())
                .setMemberEmail(member.getEmail())
                .build();
            MemberResponse response = MemberResponse.newBuilder().setMember(protoMember).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
           // else {
           //     responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Username is already taken").asRuntimeException());
           //     LOG.warn("Registration failed: Username is already taken");
           // }

        } catch(Exception e)
            {
                LOG.error("Failed to register member", e);
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Internal server error during registration")
                        .asRuntimeException());
            }
        }
    @Override
    public void login(LoginRequest request, StreamObserver<MemberResponse> responseObserver) {
        try {
            Optional<dk.via.sep3.entities.Member> memberOpt = dataMemberService.login(request.getMemberName(), request.getPassword());
            if (memberOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Wrong user name or password").asRuntimeException());
                LOG.warn("Login failed: invalid credentials for {}", request.getMemberName());
                return;
            }

            dk.via.sep3.entities.Member member = memberOpt.get();
            dk.via.sep3.generated.Member protoMember = dk.via.sep3.generated.Member.newBuilder()
                .setFirstName(member.getFirstName())
                .setLastName(member.getLastName())
                .setMemberName(member.getUsername())
                .setId(member.getMemberId())
                .setMemberEmail(member.getEmail())
                .build();

            MemberResponse response = MemberResponse.newBuilder().setMember(protoMember).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            LOG.info("Successfully logged in Member: {}", member.getUsername());

        } catch(Exception e) {
            LOG.error("Failed to login member", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error during login")
                    .asRuntimeException());
        }
    }
}


