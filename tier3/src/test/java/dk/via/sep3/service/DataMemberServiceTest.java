package dk.via.sep3.service;

import dk.via.sep3.entities.Member;
import dk.via.sep3.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class DataMemberServiceTest {

    private MemberRepository repository;
    private DataMemberService service;

    @BeforeEach
    void setUp() {
        repository = mock(MemberRepository.class);
        service = new DataMemberService(repository);
    }

    @Test
    void addMemberReturnsEmptyForExistingUsername() {
        when(repository.existsByUsername("ada")).thenReturn(true);

        assertEquals(Optional.empty(), service.addMember("Ada", "Lovelace", "ada", "secret", "ada@example.com"));
        verify(repository).existsByUsername("ada");
        verify(repository, never()).save(any(Member.class));
    }

    @Test
    void addMemberSavesAndReturnsNewMember() {
        when(repository.existsByUsername("ada")).thenReturn(false);

        Optional<Member> result = service.addMember("Ada", "Lovelace", "ada", "secret", "ada@example.com");

        assertTrue(result.isPresent());
        Member member = result.orElseThrow();
        assertEquals("Ada", member.getFirstName());
        assertEquals("Lovelace", member.getLastName());
        assertEquals("ada", member.getUsername());
        assertEquals("secret", member.getPassword());
        assertEquals("ada@example.com", member.getEmail());
        verify(repository).save(member);
    }

    @Test
    void loginDelegatesCredentials() {
        Member member = new Member("Ada", "Lovelace", "ada", "secret", "ada@example.com");
        when(repository.findByUsernameAndPassword("ada", "secret")).thenReturn(Optional.of(member));

        assertEquals(Optional.of(member), service.login("ada", "secret"));
        verify(repository).findByUsernameAndPassword("ada", "secret");
    }
}