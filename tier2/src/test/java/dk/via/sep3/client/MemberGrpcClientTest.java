package dk.via.sep3.client;

import com.google.protobuf.Empty;
import dk.via.sep3.generated.AddMemberRequest;
import dk.via.sep3.generated.MemberServiceGrpc;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(minimal))
                    .thenReturn(Empty.getDefaultInstance());

            boolean result = client.addMember(minimal);

            assertTrue(result);
        }

        @Test
        @DisplayName("adds a single valid member")
        void one_validRequest_returnsTrue() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenReturn(Empty.getDefaultInstance());

            boolean result = client.addMember(validRequest);

            assertTrue(result);
        }

        @Test
        @DisplayName("adds multiple members sequentially")
        void many_sequentialAdds_allSucceed() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(any(AddMemberRequest.class)))
                    .thenReturn(Empty.getDefaultInstance());

            boolean r1 = client.addMember(validRequest);
            boolean r2 = client.addMember(
                    AddMemberRequest.newBuilder()
                            .setMemberName("bob")
                            .setMemberEmail("bob@example.com")
                            .setPassword("secret456")
                            .setFirstName("Bob")
                            .setLastName("Jones")
                            .build()
            );
            boolean r3 = client.addMember(
                    AddMemberRequest.newBuilder()
                            .setMemberName("carol")
                            .setMemberEmail("carol@example.com")
                            .setPassword("secret789")
                            .setFirstName("Carol")
                            .setLastName("White")
                            .build()
            );

            assertTrue(r1);
            assertTrue(r2);
            assertTrue(r3);
        }

        @Test
        @DisplayName("returns false when member already exists")
        void boundary_alreadyExists_returnsFalse() {
            when(stub.withDeadlineAfter(anyLong(), any()))
                    .thenReturn(stub);
            when(stub.addMember(validRequest))
                    .thenThrow(new StatusRuntimeException(Status.ALREADY_EXISTS));

            boolean result = client.addMember(validRequest);

            assertFalse(result);
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
}
