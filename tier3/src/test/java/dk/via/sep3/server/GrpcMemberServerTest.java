package dk.via.sep3.server;

import dk.via.sep3.entities.Member;
import dk.via.sep3.service.DataMemberService;
import dk.via.sep3.support.RecordingStreamObserver;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrpcMemberServerTest {

    private DataMemberService service;
    private GrpcMemberServer server;

    @BeforeEach
    void setUp() {
        service = mock(DataMemberService.class);
        server = new GrpcMemberServer(service);
    }

    @Test
    void addMemberMapsRequestAndCreatedMember() {
        Member member = member();
        when(service.addMember("Ada", "Lovelace", "ada", "secret", "ada@example.com"))
                .thenReturn(Optional.of(member));
        var request = dk.via.sep3.generated.AddMemberRequest.newBuilder()
                .setFirstName("Ada").setLastName("Lovelace").setMemberName("ada")
                .setPassword("secret").setMemberEmail("ada@example.com").build();
        RecordingStreamObserver<dk.via.sep3.generated.MemberResponse> observer = new RecordingStreamObserver<>();

        server.addMember(request, observer);

        var response = observer.values().get(0).getMember();
        assertEquals(7, response.getId());
        assertEquals("Ada", response.getFirstName());
        assertEquals("Lovelace", response.getLastName());
        assertEquals("ada", response.getMemberName());
        assertEquals("ada@example.com", response.getMemberEmail());
        assertTrue(observer.completed());
        verify(service).addMember("Ada", "Lovelace", "ada", "secret", "ada@example.com");
    }

    @Test
    void duplicateUsernameReturnsAlreadyExists() {
        when(service.addMember("Ada", "Lovelace", "ada", "secret", "ada@example.com"))
                .thenReturn(Optional.empty());
        RecordingStreamObserver<dk.via.sep3.generated.MemberResponse> observer = new RecordingStreamObserver<>();

        server.addMember(dk.via.sep3.generated.AddMemberRequest.newBuilder()
                .setFirstName("Ada").setLastName("Lovelace").setMemberName("ada")
                .setPassword("secret").setMemberEmail("ada@example.com").build(), observer);

        assertEquals(Status.Code.ALREADY_EXISTS, Status.fromThrowable(observer.error()).getCode());
        assertFalse(observer.completed());
    }

    @Test
    void loginReturnsMemberOrNotFoundForInvalidCredentials() {
        when(service.login("ada", "secret")).thenReturn(Optional.of(member()));
        RecordingStreamObserver<dk.via.sep3.generated.MemberResponse> signedIn = new RecordingStreamObserver<>();
        server.login(dk.via.sep3.generated.LoginRequest.newBuilder()
                .setMemberName("ada").setPassword("secret").build(), signedIn);
        assertEquals("ada", signedIn.values().get(0).getMember().getMemberName());
        assertTrue(signedIn.completed());

        when(service.login("ada", "wrong")).thenReturn(Optional.empty());
        RecordingStreamObserver<dk.via.sep3.generated.MemberResponse> rejected = new RecordingStreamObserver<>();
        server.login(dk.via.sep3.generated.LoginRequest.newBuilder()
                .setMemberName("ada").setPassword("wrong").build(), rejected);
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(rejected.error()).getCode());
    }

    @Test
    void serviceFailureMapsToInternalStatus() {
        when(service.login("ada", "secret")).thenThrow(new IllegalStateException("Database unavailable"));
        RecordingStreamObserver<dk.via.sep3.generated.MemberResponse> observer = new RecordingStreamObserver<>();

        server.login(dk.via.sep3.generated.LoginRequest.newBuilder()
                .setMemberName("ada").setPassword("secret").build(), observer);

        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(observer.error()).getCode());
    }

    private static Member member() {
        Member member = new Member("Ada", "Lovelace", "ada", "secret", "ada@example.com");
        member.setMemberId(7);
        return member;
    }
}