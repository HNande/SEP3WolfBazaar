package dk.via.sep3.client;

import dk.via.sep3.generated.AddMemberRequest;
import dk.via.sep3.generated.Member;
import dk.via.sep3.generated.MemberResponse;
import dk.via.sep3.generated.MemberServiceGrpc;
import dk.via.sep3.generated.LoginRequest;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberGrpcClientTest {

    private MemberServiceGrpc.MemberServiceBlockingStub stub;
    private MemberGrpcClient client;

    @BeforeEach
    void setUp() {
        stub = mock(MemberServiceGrpc.MemberServiceBlockingStub.class);
        client = new MemberGrpcClient(stub);
    }


    @Nested
    @DisplayName("addMember")
    class AddMemberTests {

        private AddMemberRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = AddMemberRequest.newBuilder()
                    .setMemberName("alice")
                    .setMemberEmail("alice@example.com")
                    .setPassword("secret123")
                    .setFirstName("Alice")
                    .setLastName("Smith")
                    .build();
        }

        @Test
        @DisplayName("adds member with minimal request")
        void zero_minimalRequest_succeeds() {
            AddMemberRequest minimal = AddMemberRequest.newBuilder().build();
            Member member = Member.newBuilder().setId(1).build();
            MemberResponse response = MemberResponse.newBuilder().setMember(member).build();
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(minimal))
                    .thenReturn(response);

            Optional<MemberResponse> result = client.addMember(minimal);

            assertTrue(result.isPresent());
            assertEquals(1, result.get().getMember().getId());
        }

        @Test
        @DisplayName("adds a single valid member")
        void one_validRequest_returnsTrue() {
            Member member = Member.newBuilder()
                    .setId(1)
                    .setMemberName("alice")
                    .setMemberEmail("alice@example.com")
                    .setFirstName("Alice")
                    .setLastName("Smith")
                    .build();
            MemberResponse response = MemberResponse.newBuilder().setMember(member).build();
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenReturn(response);

            Optional<MemberResponse> result = client.addMember(validRequest);

            assertTrue(result.isPresent());
            assertEquals(1, result.get().getMember().getId());
        }

        @Test
        @DisplayName("adds multiple members sequentially")
        void many_sequentialAdds_allSucceed() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(any(AddMemberRequest.class)))
                    .thenReturn(MemberResponse.newBuilder()
                            .setMember(Member.newBuilder().setId(1).build())
                            .build());

            Optional<MemberResponse> r1 = client.addMember(validRequest);
            Optional<MemberResponse> r2 = client.addMember(
                    AddMemberRequest.newBuilder()
                            .setMemberName("bob")
                            .setMemberEmail("bob@example.com")
                            .setPassword("secret456")
                            .setFirstName("Bob")
                            .setLastName("Jones")
                            .build()
            );
            Optional<MemberResponse> r3 = client.addMember(
                    AddMemberRequest.newBuilder()
                            .setMemberName("carol")
                            .setMemberEmail("carol@example.com")
                            .setPassword("secret789")
                            .setFirstName("Carol")
                            .setLastName("White")
                            .build()
            );

            assertTrue(r1.isPresent());
            assertTrue(r2.isPresent());
            assertTrue(r3.isPresent());
        }

        @Test
        @DisplayName("returns empty when member already exists")
        void boundary_alreadyExists_returnsEmpty() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenThrow(new StatusRuntimeException(Status.ALREADY_EXISTS));

            Optional<MemberResponse> result = client.addMember(validRequest);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws InvalidRequestException on INVALID_ARGUMENT")
        void exceptional_invalidArgument_throwsInvalidRequestException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenThrow(new StatusRuntimeException(
                            Status.INVALID_ARGUMENT.withDescription("Email format invalid")));

            InvalidRequestException ex = assertThrows(
                    InvalidRequestException.class,
                    () -> client.addMember(validRequest)
            );

            assertEquals("Email format invalid", ex.getMessage());
        }

        @Test
        @DisplayName("uses default message when INVALID_ARGUMENT has blank description")
        void exceptional_invalidArgumentBlankDescription_usesDefaultMessage() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenThrow(new StatusRuntimeException(
                            Status.INVALID_ARGUMENT.withDescription("")));

            InvalidRequestException ex = assertThrows(
                    InvalidRequestException.class,
                    () -> client.addMember(validRequest)
            );

            assertEquals("Invalid request", ex.getMessage());
        }

        @Test
        @DisplayName("throws UpstreamException on INTERNAL error")
        void exceptional_internalError_throwsUpstreamException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            UpstreamException ex = assertThrows(
                    UpstreamException.class,
                    () -> client.addMember(validRequest)
            );

            assertTrue(ex.getMessage().contains("add member"));
            assertTrue(ex.getMessage().contains("INTERNAL"));
            assertNotNull(ex.getCause());
        }

        @Test
        @DisplayName("throws UpstreamException on UNAVAILABLE error")
        void exceptional_unavailable_throwsUpstreamException() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

            assertThrows(
                    UpstreamException.class,
                    () -> client.addMember(validRequest)
            );
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @BeforeEach
        void configureStub() {
            when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);
        }

        private final LoginRequest request = LoginRequest.newBuilder()
                .setMemberName("alice")
                .setPassword("secret123")
                .build();

        @Test
        @DisplayName("returns the response for valid credentials")
        void validCredentials_returnResponse() {
            MemberResponse expected = MemberResponse.newBuilder()
                    .setMember(Member.newBuilder().setId(4).setMemberName("alice").build())
                    .build();
            when(stub.login(request)).thenReturn(expected);

            assertEquals(Optional.of(expected), client.login(request));
            verify(stub).login(request);
        }

        @Test
        @DisplayName("returns empty when credentials are not found")
        void wrongCredentials_returnEmpty() {
            when(stub.login(request)).thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

            assertTrue(client.login(request).isEmpty());
        }

        @Test
        @DisplayName("translates invalid arguments and upstream errors")
        void errors_areTranslated() {
            doThrow(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Password is invalid")))
                    .when(stub).login(request);
            InvalidRequestException invalid = assertThrows(InvalidRequestException.class,
                    () -> client.login(request));
            assertEquals("Password is invalid", invalid.getMessage());

            doThrow(new StatusRuntimeException(Status.UNAVAILABLE)).when(stub).login(request);
            assertThrows(UpstreamException.class, () -> client.login(request));
        }
    }
}
